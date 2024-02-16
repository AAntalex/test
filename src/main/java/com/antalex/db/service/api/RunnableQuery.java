package com.antalex.db.service.api;

public interface RunnableQuery {
    void execute() throws Exception;
    RunnableQuery bind(Object o) throws Exception;
    RunnableQuery addBatch() throws Exception;
}
