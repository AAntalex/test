package com.antalex.db.service.impl;

import com.antalex.db.model.Shard;
import com.antalex.db.service.api.RunnableTask;

import javax.persistence.EntityTransaction;
import java.util.*;

public class SharedEntityTransaction implements EntityTransaction {
    private SharedEntityTransaction parentTransaction;
    private boolean active;
    private boolean completed;

    private List<RunnableTask> tasks = new ArrayList<>();
    private Map<Short, RunnableTask> currentTasks = new HashMap<>();
    private Map<Short, Chunk> chunks = new HashMap<>();

    @Override
    public void begin() {
        this.active = true;
    }

    @Override
    public void rollback() {
        this.completed = true;
    }

    @Override
    public void commit() {
        this.tasks.forEach(RunnableTask::run);
        this.tasks.forEach(task -> {
            task.waitTask();
            if (Objects.nonNull(task.getError())) {
                this.hasError = true;
            }
        });

        this.completed = true;
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
        return Optional.ofNullable(currentTasks.get(shard.getId()))
                .orElse(
                        Optional.ofNullable(chunks.get(shard.getId()))
                                .filter(it -> limitParallel)
                                .map(Chunk::getTask)
                                .map(task -> {
                                    currentTasks.put(shard.getId(), task);
                                    return task;
                                })
                                .orElse(null)
                );
    }

    public void addTask(Shard shard, RunnableTask task) {
        currentTasks.put(shard.getId(), task);
        tasks.add(task);
        Optional.ofNullable(chunks.get(shard.getId()))
                .orElseGet(() -> {
                    Chunk chunk = new Chunk();
                    chunks.put(shard.getId(), chunk);
                    return chunk;
                })
                .addTask(task);
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
