package com.antalex.db.service.impl;

import com.antalex.db.model.Shard;
import com.antalex.db.service.api.TransactionalTask;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.EntityTransaction;
import java.util.*;

public class SharedEntityTransaction implements EntityTransaction {
    private static final String SQL_ERROR_TEXT = "Ошибки при выполнении запроса: ";
    private static final String SQL_ERROR_COMMIT_TEXT = "Ошибки при подтверждении транзакции: ";
    private static final String SQL_ERROR_ROLLBACK_TEXT = "Ошибки при откате транзакции: ";
    private static final String SQL_ERROR_PREFIX = "\n\t\t";
    private static final String TASK_PREFIX = "\n\t";

    private SharedEntityTransaction parentTransaction;
    private boolean active;
    private boolean completed;
    private boolean hasError;
    private String error;
    private String errorCommit;
    private UUID uid;
    private Boolean parallelRun;

    private List<TransactionalTask> tasks = new ArrayList<>();
    private Map<Integer, TransactionalTask> currentTasks = new HashMap<>();
    private Map<Integer, Bucket> buckets = new HashMap<>();

    SharedEntityTransaction(Boolean parallelRun) {
        this.parallelRun = parallelRun;
    }

    @Override
    public void begin() {
        if (!this.active) {
            this.uid = UUID.randomUUID();
            this.active = true;
        }
    }

    @Override
    public void rollback() {
        tasks.clear();
        currentTasks.clear();
        buckets.clear();
        this.completed = true;
    }

    @Override
    public void commit() {
        this.tasks.forEach(task -> task.run(parallelRun && this.tasks.size() > 1));
        this.tasks.forEach(task -> {
            task.waitTask();
            this.error = processTask(task, task.getError(), this.error, SQL_ERROR_TEXT);
        });


        getCurrentTask().addStepBeforeCommit();
        getCurrentTask().addStepAfterRollback();



        this.tasks.forEach(task -> task.completion(this.hasError));
        this.tasks.forEach(TransactionalTask::finish);
        this.tasks.forEach(task ->
                this.errorCommit =
                        processTask(
                                task,
                                task.getErrorCompletion(),
                                this.errorCommit,
                                this.hasError ? SQL_ERROR_ROLLBACK_TEXT : SQL_ERROR_COMMIT_TEXT
                        )
        );
        this.completed = true;
        if (this.hasError) {
            throw new RuntimeException(
                    Optional.ofNullable(this.error)
                            .map(it -> it.concat(StringUtils.LF))
                            .orElse(StringUtils.EMPTY)
                            .concat(
                                    Optional.ofNullable(this.errorCommit)
                                            .orElse(StringUtils.EMPTY)
                            )
            );
        }
    }

    @Override
    public void setRollbackOnly() {

    }

    @Override
    public boolean getRollbackOnly() {
        return false;
    }

    @Override
    public boolean isActive() {
        return this.active;
    }

    public SharedEntityTransaction getParentTransaction() {
        return parentTransaction;
    }

    public void setParentTransaction(SharedEntityTransaction parentTransaction) {
        this.parentTransaction = parentTransaction;
    }

    public boolean isCompleted() {
        return this.completed;
    }

    public UUID getUid() {
        return uid;
    }

    public TransactionalTask getCurrentTask(Shard shard, boolean limitParallel) {
        return Optional.ofNullable(currentTasks.get(shard.getHashCode()))
                .orElse(
                        Optional.ofNullable(buckets.get(shard.getHashCode()))
                                .filter(it -> limitParallel)
                                .map(Bucket::getTask)
                                .map(task -> {
                                    currentTasks.put(shard.getHashCode(), task);
                                    return task;
                                })
                                .orElse(null)
                );
    }

    public void addTask(Shard shard, TransactionalTask task) {
        currentTasks.put(shard.getHashCode(), task);
        tasks.add(task);
        Optional.ofNullable(buckets.get(shard.getHashCode()))
                .orElseGet(() -> {
                    Bucket chunk = new Bucket();
                    buckets.put(shard.getHashCode(), chunk);
                    return chunk;
                })
                .addTask(task);

        int chunkSize = buckets.get(shard.getHashCode()).chunkSize();
        task.setName(
                "TRN: " + this.uid + "(shard: " + shard.getName() +
                        (chunkSize > 1 ? ", chunk: " + chunkSize : ")")
        );
    }

    public void addParallel() {
        currentTasks.clear();
    }

    private String processTask(
            TransactionalTask task,
            String errorTask,
            String errorText,
            String errorPrefix)
    {
        if (Objects.nonNull(errorTask)) {
            this.hasError = true;
            return Optional.ofNullable(errorText)
                    .orElse(errorPrefix)
                    .concat(TASK_PREFIX)
                    .concat(task.getName())
                    .concat(":" + SQL_ERROR_PREFIX)
                    .concat(errorTask.replace(StringUtils.LF, SQL_ERROR_PREFIX));
        }
        return errorText;
    }

    private class Bucket {
        private List<TransactionalTask> chunks = new ArrayList<>();
        private int currentIndex;

        int chunkSize() {
            return chunks.size();
        }

        void addTask(TransactionalTask task) {
            chunks.add(task);
        }

        TransactionalTask getTask() {
            if (chunks.isEmpty()) {
                return null;
            }
            if (currentIndex >= chunks.size()) {
                currentIndex = 0;
            }
            return chunks.get(currentIndex++);
        }
    }
}
