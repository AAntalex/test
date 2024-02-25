package com.antalex.db.service.api;

public interface TransactionalQuery {
    TransactionalQuery bind(Object o);
    TransactionalQuery addBatch();
    String getQuery();
    Object getResult();
}
