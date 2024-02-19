package com.antalex.db.service.api;

import com.antalex.db.model.enums.QueryType;

public interface RunnableTask {
    void confirm() throws Exception;
    void revoke() throws Exception;
    void submit();
    void waitTask();
    RunnableQuery addQuery(String query, QueryType queryType);
}
