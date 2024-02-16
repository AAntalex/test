package com.antalex.db.service.api;

public interface RunnableQueryTask {
    String getName();
    void setName(String name);
    void confirm() throws Exception;
    void revoke() throws Exception;
    void submit(Runnable target);
    void waitTask();
    RunnableQuery getQuery(String query) throws Exception;
}
