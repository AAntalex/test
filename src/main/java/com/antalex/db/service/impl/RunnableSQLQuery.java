package com.antalex.db.service.impl;

import com.antalex.db.service.api.RunnableQuery;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class RunnableSQLQuery implements RunnableQuery {
    private PreparedStatement preparedStatement;
    int currentIndex = 0;

    RunnableSQLQuery(PreparedStatement preparedStatement) {
        this.preparedStatement = preparedStatement;
    }

    @Override
    public void execute() throws SQLException {
        preparedStatement.executeBatch();
    }

    @Override
    public RunnableQuery bind(Object o) throws SQLException {
        preparedStatement.setObject(++currentIndex, o);
        return this;
    }

    @Override
    public RunnableQuery addBatch() throws SQLException {
        preparedStatement.addBatch();
        return this;
    }
}
