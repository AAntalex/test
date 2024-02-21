package com.antalex.db.service.impl;

import com.antalex.db.service.SharedTransactionManager;
import org.springframework.stereotype.Component;

import javax.persistence.EntityTransaction;
import java.util.Optional;

@Component
public class SharedTransactionManagerImpl implements SharedTransactionManager {
    private ThreadLocal<SharedEntityTransaction> transaction = new ThreadLocal<>();

    @Override
    public EntityTransaction getTransaction() {
        return Optional.ofNullable(this.transaction.get())
                .filter(it -> !it.isCompleted())
                .orElseGet(() -> {
                    this.transaction.set(
                            Optional.ofNullable(this.transaction.get())
                                    .map(SharedEntityTransaction::getParentTransaction)
                                    .orElse(new SharedEntityTransaction())
                    );
                    return this.transaction.get();
                });
    }

    @Override
    public EntityTransaction getCurrentTransaction() {
        return this.transaction.get();
    }

    @Override
    public void setAutonomousTransaction() {
        SharedEntityTransaction transaction = new SharedEntityTransaction();
        transaction.setParentTransaction(this.transaction.get());
        this.transaction.set(transaction);
    }

    public SharedEntityTransaction createTransaction() {
        return null;
    }

}
