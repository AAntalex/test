package com.antalex.db.service.api;

import com.antalex.db.model.enums.QueryType;

import java.util.concurrent.ExecutorService;

public interface TransactionalQuery {
    TransactionalQuery bind(Object o);
    TransactionalQuery addBatch();
    void addRelatedQuery(TransactionalQuery query);
    String getQuery();
    Object getResult();
    QueryType getQueryType();
    int getCount();
    void execute();
    void setMainQuery(TransactionalQuery mainQuery);
    TransactionalQuery getMainQuery();
    void setExecutorService(ExecutorService executorService);
    public void setParallelRun(Boolean parallelRun);
}
