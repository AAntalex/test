package com.antalex.db.service.api;

import java.util.concurrent.ExecutorService;

public interface TransactionalExternalTaskFactory {
    void setExecutorService(ExecutorService executorService);
    TransactionalTask createTask();
}
