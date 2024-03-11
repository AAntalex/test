package com.antalex.db.service.api;

import com.antalex.db.model.enums.QueryType;

public interface TransactionalQuery {
    TransactionalQuery bind(Object o);
    TransactionalQuery addBatch();
    String getQuery();
    Object getResult();
    QueryType getQueryType();
    int getCount();
    void execute();
}
