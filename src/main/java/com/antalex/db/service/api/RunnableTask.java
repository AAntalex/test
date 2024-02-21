package com.antalex.db.service.api;

import com.antalex.db.model.enums.QueryType;

public interface RunnableTask {
    void confirm() throws Exception;
    void revoke() throws Exception;
    void run();
    void waitTask();
    boolean isRunning();
    RunnableQuery addQuery(String query, QueryType queryType);
    RunnableQuery addQuery(String query, QueryType queryType, String name);
    void addStep(Runnable target);
    void addStep(Runnable target, String name);
    String getName();
    void setName(String name);
    String getError();
}
