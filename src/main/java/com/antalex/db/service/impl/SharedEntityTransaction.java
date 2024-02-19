package com.antalex.db.service.impl;

import com.antalex.db.model.Shard;
import com.antalex.db.service.api.RunnableTask;

import javax.persistence.EntityTransaction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SharedEntityTransaction implements EntityTransaction {
    private EntityTransaction parentTransaction;
    private boolean active;

    private List<RunnableTask> tasks = new ArrayList<>();
    private Map<Shard, RunnableTask> currentTasks = new HashMap<>();

    @Override
    public void begin() {
        this.active = true;
    }

    @Override
    public void rollback() {

    }

    @Override
    public void commit() {

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

    public EntityTransaction getParentTransaction() {
        return parentTransaction;
    }

    public void setParentTransaction(EntityTransaction parentTransaction) {
        this.parentTransaction = parentTransaction;
    }
}
