package com.antalex.db.service.impl;

import com.antalex.db.service.api.RunnableExternalTaskFactory;
import com.antalex.db.service.api.RunnableTask;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

@Component
public class RunnableExternalTaskFactoryImpl implements RunnableExternalTaskFactory {
    private ExecutorService executorService;

    @Override
    public RunnableTask createTask() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }
}
