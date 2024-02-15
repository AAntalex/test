package com.antalex.db.service.impl;

import javax.persistence.EntityTransaction;

public class SharedEntityTransaction implements EntityTransaction {
    private EntityTransaction parentTransaction;
    private boolean active;

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
