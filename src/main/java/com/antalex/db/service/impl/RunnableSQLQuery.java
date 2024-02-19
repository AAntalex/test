package com.antalex.db.service.impl;

import com.antalex.db.model.enums.QueryType;
import com.antalex.db.service.api.RunnableQuery;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RunnableSQLQuery implements RunnableQuery, Runnable {
    private PreparedStatement preparedStatement;
    private QueryType queryType;
    private ResultSet result;
    private int currentIndex;
    private boolean isButch;

    RunnableSQLQuery(QueryType queryType, PreparedStatement preparedStatement) {
        this.queryType = queryType;
        this.preparedStatement = preparedStatement;
    }

    @Override
    public void run() {
        try {
            if (queryType == QueryType.DML) {
                if (this.isButch) {
                    preparedStatement.executeUpdate();
                } else {
                    preparedStatement.executeBatch();
                }
            } else {
                this.result = preparedStatement.executeQuery();
            }
        } catch (SQLException err) {
            throw new RuntimeException(err);
        }
    }

    @Override
    public RunnableQuery bind(Object o) throws SQLException {
        preparedStatement.setObject(++currentIndex, o);
        return this;
    }

    @Override
    public RunnableQuery addBatch() throws SQLException {
        this.currentIndex = 0;
        this.isButch = true;
        this.preparedStatement.addBatch();
        return this;
    }

    @Override
    public String getQuery() {
        return preparedStatement.toString();
    }

    @Override
    public ResultSet getResult() {
        return result;
    }
}
