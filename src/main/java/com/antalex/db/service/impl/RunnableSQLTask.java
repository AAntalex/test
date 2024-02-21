package com.antalex.db.service.impl;

import com.antalex.db.model.enums.QueryType;
import com.antalex.db.service.abstractive.AbstractRunnableTask;
import com.antalex.db.service.api.RunnableQuery;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class RunnableSQLTask extends AbstractRunnableTask {
    private Connection connection;
    private Map<String, RunnableQuery> queries = new HashMap<>();

    RunnableSQLTask(Connection connection, ExecutorService executorService) {
        this.connection = connection;
        this.executorService = executorService;
    }

    @Override
    public void confirm() throws SQLException {
        if (!this.connection.isClosed()) {
            if (!this.connection.getAutoCommit()) {


                this.connection.commit();
            }
            this.connection.close();
        }
    }

    @Override
    public void revoke() throws SQLException {
        if (!this.connection.isClosed()) {
            if (!this.connection.getAutoCommit()) {
                this.connection.rollback();
            }
            this.connection.close();
        }
    }

    @Override
    public RunnableQuery addQuery(String query, QueryType queryType, String name) {
        RunnableQuery runnableQuery = this.queries.get(query);
        if (runnableQuery == null) {
            try {
                if (queryType == QueryType.DML) {
                    connection.setAutoCommit(false);
                }
                runnableQuery = new RunnableSQLQuery(queryType, connection.prepareStatement(query));
            } catch (SQLException err) {
                throw new RuntimeException(err);
            }
            this.queries.put(query, runnableQuery);
            this.addStep((Runnable) runnableQuery, name);
        }
        return runnableQuery;
    }

    public Connection getConnection() {
        return connection;
    }
}
