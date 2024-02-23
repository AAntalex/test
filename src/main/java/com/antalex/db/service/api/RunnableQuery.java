package com.antalex.db.service.api;

public interface RunnableQuery {
    RunnableQuery bind(Object o);
    RunnableQuery addBatch();
    String getQuery();
    Object getResult();
}
