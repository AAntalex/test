package com.antalex.db.service.impl;

import com.antalex.db.model.Shard;
import com.antalex.db.service.api.RunnableExternalTaskFactory;
import com.antalex.db.service.api.RunnableSQLTaskFactory;
import com.antalex.db.service.api.RunnableTask;

import javax.persistence.EntityTransaction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SharedEntityTransaction implements EntityTransaction {
    private SharedEntityTransaction parentTransaction;
    private boolean active;
    private boolean completed;
    private RunnableSQLTaskFactory runnableSQLTaskFactory;
    private RunnableExternalTaskFactory runnableExternalTaskFactory;

    private List<RunnableTask> tasks = new ArrayList<>();
    private Map<Short, RunnableTask> currentTasks = new HashMap<>();

    SharedEntityTransaction(RunnableSQLTaskFactory runnableSQLTaskFactory,
                            RunnableExternalTaskFactory runnableExternalTaskFactory)
    {
        this.runnableExternalTaskFactory = runnableExternalTaskFactory;
        this.runnableSQLTaskFactory = runnableSQLTaskFactory;
    }

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

    public RunnableTask getTask(Shard shard) {
        RunnableTask runnableTask = currentTasks.get(shard.getId());
        if (runnableTask == null) {
            runnableTask = shard.getExternal() ?
                    runnableExternalTaskFactory.createTask() :
                    runnableSQLTaskFactory.createTask();
            currentTasks.put(shard.getId(), runnableTask);
            tasks.add(runnableTask);
        }
        return runnableTask;
    }
}
