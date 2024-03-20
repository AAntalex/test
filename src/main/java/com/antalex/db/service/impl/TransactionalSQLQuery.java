package com.antalex.db.service.impl;

import com.antalex.db.model.enums.QueryType;
import com.antalex.db.service.abstractive.AbstractTransactionalQuery;
import com.antalex.db.service.api.ResultQuery;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TransactionalSQLQuery extends AbstractTransactionalQuery {
    private PreparedStatement preparedStatement;

    TransactionalSQLQuery(String query, QueryType queryType, PreparedStatement preparedStatement) {
        this.preparedStatement = preparedStatement;
        this.query = query;
        this.queryType = queryType;
    }

    @Override
    public void bindOriginal(int idx, Object o) throws SQLException {
        preparedStatement.setObject(idx, o);
    }

    @Override
    public void addBatchOriginal() throws SQLException {
        this.preparedStatement.addBatch();
    }

    @Override
    public int[] executeBatch() throws SQLException {
        return this.preparedStatement.executeBatch();
    }

    @Override
    public int executeUpdate() throws SQLException {
        return this.preparedStatement.executeUpdate();
    }

    @Override
    public ResultQuery executeQuery() throws SQLException {
        this.result = new ResultSQLQuery(this.preparedStatement.executeQuery());
        return result;
    }
}
