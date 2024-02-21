package com.antalex.db.service.impl;

import com.antalex.db.service.api.RunnableSQLTaskFactory;
import com.antalex.db.service.api.RunnableTask;

import java.sql.Connection;
import java.util.concurrent.ExecutorService;

public class RunnableSQLTaskFactoryImpl implements RunnableSQLTaskFactory {
    private ExecutorService executorService;

    @Override
    public RunnableTask createTask(Connection connection) {
        return new RunnableSQLTask(connection, executorService);
    }

    @Override
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }
}
