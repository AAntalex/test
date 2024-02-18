package com.antalex.db.service.api;

public interface RunnableQuery {
    Object execute() throws Exception;
    Object executeQuery() throws Exception;
    RunnableQuery bind(Object o) throws Exception;
    RunnableQuery addBatch() throws Exception;
}
