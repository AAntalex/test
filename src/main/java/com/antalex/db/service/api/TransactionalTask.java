package com.antalex.db.service.api;

import com.antalex.db.model.Shard;
import com.antalex.db.model.enums.QueryType;

import java.util.List;

public interface TransactionalTask {
    void commit() throws Exception;
    void rollback() throws Exception;
    Boolean needCommit();
    void completion(boolean rollback);
    void finish();
    void run(Boolean parallelRun);
    void waitTask();
    TransactionalQuery createQuery(String query, QueryType queryType);
    TransactionalQuery addQuery(String query, QueryType queryType);
    TransactionalQuery addQuery(String query, QueryType queryType, String name);
    void addStep(Runnable target);
    void addStep(Runnable target, String name);
    void addStepBeforeRollback(Runnable target);
    void addStepBeforeRollback(Runnable target, String name);
    void addStepBeforeCommit(Runnable target);
    void addStepBeforeCommit(Runnable target, String name);
    void addStepAfterRollback(Runnable target);
    void addStepAfterRollback(Runnable target, String name);
    void addStepAfterCommit(Runnable target);
    void addStepAfterCommit(Runnable target, String name);
    String getName();
    void setName(String name);
    void setParallelCommit(boolean parallelCommit);
    String getError();
    String getErrorCompletion();
    TransactionalTask getMainTask();
    void setMainTask(TransactionalTask task);
    List<TransactionalQuery> getDmlQueries();
}
