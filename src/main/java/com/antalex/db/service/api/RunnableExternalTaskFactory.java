package com.antalex.db.service.api;

import java.util.concurrent.ExecutorService;

public interface RunnableExternalTaskFactory {
    void setExecutorService(ExecutorService executorService);
    RunnableTask createTask();
}
