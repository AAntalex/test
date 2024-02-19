package com.antalex.db.service.api;

public interface RunnableQuery {
    RunnableQuery bind(Object o) throws Exception;
    RunnableQuery addBatch() throws Exception;
    String getQuery();
    Object getResult();
}
