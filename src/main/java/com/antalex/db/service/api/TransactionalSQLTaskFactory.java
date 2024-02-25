package com.antalex.db.service.api;

import java.sql.Connection;
import java.util.concurrent.ExecutorService;

public interface TransactionalSQLTaskFactory {
    void setExecutorService(ExecutorService executorService);
    TransactionalTask createTask(Connection connection);
    void setParallelCommit(boolean parallelCommit);
}
