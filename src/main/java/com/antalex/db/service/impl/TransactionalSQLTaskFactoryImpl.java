package com.antalex.db.service.impl;

import com.antalex.db.service.api.TransactionalSQLTaskFactory;
import com.antalex.db.service.api.TransactionalTask;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.concurrent.ExecutorService;

@Component
public class TransactionalSQLTaskFactoryImpl implements TransactionalSQLTaskFactory {
    private ExecutorService executorService;
    private boolean parallelCommit;

    @Override
    public TransactionalTask createTask(Connection connection) {
        return new TransactionalSQLTask(connection, executorService);
    }

    @Override
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public void setParallelCommit(boolean parallelCommit) {
        this.parallelCommit = parallelCommit;
    }
}
