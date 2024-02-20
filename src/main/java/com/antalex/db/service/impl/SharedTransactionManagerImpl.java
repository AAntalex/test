package com.antalex.db.service.impl;

import com.antalex.db.service.SharedTransactionManager;
import com.antalex.db.service.api.SharedTransactionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityTransaction;

@Component
public class SharedTransactionManagerImpl implements SharedTransactionManager {
    @Autowired
    private SharedTransactionFactory sharedTransactionFactory;

    private ThreadLocal<SharedEntityTransaction> transaction = new ThreadLocal<>();

    @Override
    public EntityTransaction getTransaction() {
        SharedEntityTransaction transaction = this.transaction.get();
        if (transaction == null || transaction.isCompleted()) {
            transaction = sharedTransactionFactory.createTransaction();
            this.transaction.set(transaction);
        }
        return transaction;
    }

    @Override
    public EntityTransaction getCurrentTransaction() {
        return this.transaction.get();
    }

    @Override
    public void setAutonomousTransaction() {
        SharedEntityTransaction transaction = sharedTransactionFactory.createTransaction();
        transaction.setParentTransaction(this.transaction.get());
        this.transaction.set(transaction);
    }
}
