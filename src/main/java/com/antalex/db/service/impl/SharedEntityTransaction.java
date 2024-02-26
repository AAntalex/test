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

    private List<TransactionalTask> tasks = new ArrayList<>();
    private Map<Integer, TransactionalTask> currentTasks = new HashMap<>();
    private Map<Integer, Chunk> chunks = new HashMap<>();

    @Override
    public void begin() {
        this.active = true;
    }

    @Override
    public void rollback() {
        tasks.clear();
        currentTasks.clear();
        chunks.clear();
        this.completed = true;
    }

    @Override
    public void commit() {
        this.tasks.forEach(TransactionalTask::run);
        this.tasks.forEach(task -> {
            task.waitTask();
            this.error = processTask(task, task.getError(), this.error, SQL_ERROR_TEXT);
        });
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

    public TransactionalTask getCurrentTask(Shard shard, boolean limitParallel) {
        return Optional.ofNullable(currentTasks.get(shard.getHashCode()))
                .orElse(
                        Optional.ofNullable(chunks.get(shard.getHashCode()))
                                .filter(it -> limitParallel)
                                .map(Chunk::getTask)
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
        Optional.ofNullable(chunks.get(shard.getHashCode()))
                .orElseGet(() -> {
                    Chunk chunk = new Chunk();
                    chunks.put(shard.getHashCode(), chunk);
                    return chunk;
                })
                .addTask(task);
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

    private class Chunk {
        private List<TransactionalTask> chunks = new ArrayList<>();
        private int currentIndex;

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
