package com.antalex.service.impl;

import com.antalex.db.model.Shard;
import com.antalex.db.service.api.TransactionalExternalTaskFactory;
import com.antalex.db.service.api.TransactionalTask;

import java.util.concurrent.ExecutorService;

public class TestTaskFactoryImpl implements TransactionalExternalTaskFactory {
    private ExecutorService executorService;

    @Override
    public TransactionalTask createTask(Shard shard) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setExecutorService(ExecutorService executorService) {
        System.out.println("BBB setExecutorService");
        this.executorService = executorService;
    }
}
