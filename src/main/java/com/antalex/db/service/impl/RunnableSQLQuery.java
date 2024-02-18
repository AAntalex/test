package com.antalex.db.service.impl;

import com.antalex.db.service.api.RunnableQuery;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RunnableSQLQuery implements RunnableQuery {
    private PreparedStatement preparedStatement;
    private int currentIndex;
    private boolean isButch;

    RunnableSQLQuery(PreparedStatement preparedStatement) {
        this.preparedStatement = preparedStatement;
    }

    @Override
    public Object execute() throws SQLException {
        return this.isButch ? preparedStatement.executeUpdate() : preparedStatement.executeBatch();
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        return preparedStatement.executeQuery();
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
}
