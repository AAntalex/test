package com.antalex.db.service.impl;

import com.antalex.db.model.enums.QueryType;
import com.antalex.db.service.api.TransactionalQuery;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TransactionalSQLQuery implements TransactionalQuery, Runnable {
    private PreparedStatement preparedStatement;
    private QueryType queryType;
    private ResultSet result;
    private int currentIndex;
    private boolean isButch;
    private int count;
    private String query;

    TransactionalSQLQuery(String query, QueryType queryType, PreparedStatement preparedStatement) {
        this.queryType = queryType;
        this.preparedStatement = preparedStatement;
        this.query = query;
    }

    @Override
    public void run() {
        try {
            if (queryType == QueryType.DML) {
                if (this.isButch) {
                    preparedStatement.executeBatch();
                } else {
                    this.count = 1;
                    preparedStatement.executeUpdate();
                }
            } else {
                this.count = 1;
                this.result = preparedStatement.executeQuery();
            }
        } catch (SQLException err) {
            throw new RuntimeException(err);
        }
    }

    @Override
    public TransactionalQuery bind(Object o) {
        try {
            preparedStatement.setObject(++currentIndex, o);
        } catch (SQLException err) {
            throw new RuntimeException(err);
        }
        return this;
    }

    @Override
    public TransactionalQuery addBatch() {
        this.count++;
        this.currentIndex = 0;
        this.isButch = true;
        try {
            this.preparedStatement.addBatch();
        } catch (SQLException err) {
            throw new RuntimeException(err);
        }
        return this;
    }

    @Override
    public String getQuery() {
        return this.query;
    }

    @Override
    public ResultSet getResult() {
        return result;
    }

    @Override
    public QueryType getQueryType() {
        return queryType;
    }

    @Override
    public int getCount() {
        return count;
    }
}
