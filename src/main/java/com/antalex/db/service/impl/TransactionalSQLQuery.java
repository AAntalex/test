package com.antalex.db.service.impl;

import com.antalex.db.model.enums.QueryType;
import com.antalex.db.service.abstractive.AbstractTransactionalQuery;
import com.antalex.db.service.api.ResultQuery;

import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

public class TransactionalSQLQuery extends AbstractTransactionalQuery {
    private final PreparedStatement preparedStatement;
    private static final int FETCH_SIZE = 100000;

    TransactionalSQLQuery(String query, QueryType queryType, PreparedStatement preparedStatement) {
        this.preparedStatement = preparedStatement;
        this.query = query;
        this.queryType = queryType;
    }

    @Override
    public void bindOriginal(int idx, Object o) throws SQLException {
        if (o instanceof Date) {
            if (o instanceof Timestamp) {
                preparedStatement.setTimestamp(idx, new Timestamp(((Date) o).getTime()));
                return;
            }
            if (o instanceof Time) {
                preparedStatement.setTime(idx, new Time(((Date) o).getTime()));
                return;
            }
            preparedStatement.setDate(idx, new java.sql.Date(((Date) o).getTime()));
            return;
        }
        if (o instanceof Blob) {
            preparedStatement.setBlob(idx, ((Blob) o).getBinaryStream());
            return;
        }
        if (o instanceof Clob) {
            preparedStatement.setString(idx, ((Clob) o).getSubString(1, (int) ((Clob) o).length()));
            return;
        }
        if (o instanceof URL) {
            preparedStatement.setString(idx, ((URL) o).toExternalForm());
            return;
        }
        if (o instanceof LocalDateTime) {
            preparedStatement.setTimestamp(idx, Timestamp.valueOf((LocalDateTime) o));
            return;
        }
        if (o instanceof Enum) {
            preparedStatement.setString(idx, ((Enum<?>) o).name());
            return;
        }
        if (o instanceof UUID) {
            preparedStatement.setString(idx, o.toString());
            return;
        }

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
        this.preparedStatement.setFetchSize(FETCH_SIZE);
        this.result = new ResultSQLQuery(this.preparedStatement.executeQuery());
        return result;
    }
}
