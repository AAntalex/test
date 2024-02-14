package com.antalex.db.service.impl;

import javax.persistence.EntityTransaction;

public class ShardEntityTransaction implements EntityTransaction {
    @Override
    public void begin() {

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
        return false;
    }
}
