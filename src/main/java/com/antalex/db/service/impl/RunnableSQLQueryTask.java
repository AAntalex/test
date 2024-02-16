package com.antalex.db.service.impl;

import com.antalex.db.service.api.RunnableQuery;
import com.antalex.db.service.api.RunnableQueryTask;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public class RunnableSQLQueryTask implements RunnableQueryTask {
    private ExecutorService executorService;
    private Connection connection;
    private String name;
    private Future future;
    private Map<String, RunnableQuery> queries = new HashMap<>();

    private RunnableSQLQueryTask() {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
    }
    

    @Override
    public void confirm() throws SQLException {
        this.connection.commit();
    }

    @Override
    public void revoke() throws SQLException {
        this.connection.rollback();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void submit(Runnable target) {
        this.future = this.executorService.submit(target);
    }

    @Override
    public void waitTask() {
        if (this.future == null) {
            return;
        }
        try {
            this.future.get();
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    @Override
    public RunnableQuery getQuery(String query) throws SQLException {
        if (!this.queries.containsKey(query)) {
            this.queries.put(query, new RunnableSQLQuery(connection.prepareStatement(query)));
        }
        return this.queries.get(query);
    }

}
