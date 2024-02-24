package com.antalex.db.service.impl;

import com.antalex.db.model.Shard;
import com.antalex.db.service.api.RunnableTask;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.EntityTransaction;
import java.util.*;

public class SharedEntityTransaction implements EntityTransaction {
    private static final String SQL_ERROR_TEXT = "Ошибки при выполнении запроса: ";
    private static final String SQL_ERROR_COMMIT_TEXT = "Ошибки при подтверждении транзакции: ";
    private static final String SQL_ERROR_ROLLBACK_TEXT = "Ошибки при откате транзакции: ";
    private static final String SQL_ERROR_PREFIX = "   : ";

    private SharedEntityTransaction parentTransaction;
    private boolean active;
    private boolean completed;
    private boolean hasError;
    private String error;

    private List<RunnableTask> tasks = new ArrayList<>();
    private Map<Integer, RunnableTask> currentTasks = new HashMap<>();
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
        this.tasks.forEach(RunnableTask::run);
        this.tasks.forEach(task -> {
            task.waitTask();
            processTask(task, SQL_ERROR_TEXT);
        });
        this.tasks.forEach(task -> {
            try {
                if (this.hasError) {
                    task.revoke();
                } else {
                    task.confirm();
                }
            } catch (Exception err) {
                throw new RuntimeException(err);
            }
        });
        this.tasks.forEach(RunnableTask::finish);
        this.tasks.forEach(task -> processTask(task, this.hasError ? SQL_ERROR_ROLLBACK_TEXT : SQL_ERROR_COMMIT_TEXT));
        this.completed = true;
        if (this.hasError) {
            throw new RuntimeException(this.error);
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

    public RunnableTask getCurrentTask(Shard shard, boolean limitParallel) {
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

    public void addTask(Shard shard, RunnableTask task) {
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

    private void processTask(RunnableTask task, String errorText) {
        if (Objects.nonNull(task.getError())) {
            this.hasError = true;
            this.error = Optional.ofNullable(this.error)
                    .map(it -> it.concat(StringUtils.LF))
                    .orElse(errorText)
                    .concat(task.getName())
                    .concat(SQL_ERROR_PREFIX)
                    .concat(task.getError());
        }
    }

    private class Chunk {
        private List<RunnableTask> chunks = new ArrayList<>();
        private int currentIndex;

        void addTask(RunnableTask task) {
            chunks.add(task);
        }

        RunnableTask getTask() {
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
