package com.antalex.db.service.impl;

import com.antalex.db.service.api.RunnableExternalTaskFactory;
import com.antalex.db.service.api.RunnableTask;

import java.util.concurrent.ExecutorService;


public class RunnableExternalTaskFactoryImpl implements RunnableExternalTaskFactory {
    @Override
    public RunnableTask createTask() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setExecutorService(ExecutorService executorService) {
        throw new UnsupportedOperationException();
    }
}
