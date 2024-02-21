package com.antalex.db.service.api;

import java.sql.Connection;
import java.util.concurrent.ExecutorService;

public interface RunnableSQLTaskFactory {
    void setExecutorService(ExecutorService executorService);
    RunnableTask createTask(Connection connection);
}
