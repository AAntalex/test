package com.antalex.db.service.impl;

import com.antalex.db.model.enums.QueryType;
import com.antalex.db.service.abstractive.AbstractRunnableTask;
import com.antalex.db.service.api.RunnableQuery;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class RunnableSQLTask extends AbstractRunnableTask {
    private Connection connection;
    private Map<String, RunnableQuery> queries = new HashMap<>();

    private RunnableSQLTask(Connection connection) {
        this.connection = connection;
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
    public RunnableQuery addQuery(String query, QueryType queryType) {
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
            addStep((Runnable) runnableQuery);
        }
        return runnableQuery;
    }
}
