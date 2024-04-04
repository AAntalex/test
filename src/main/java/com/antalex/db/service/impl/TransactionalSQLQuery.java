package com.antalex.db.service.impl;

import com.antalex.db.model.enums.QueryType;
import com.antalex.db.service.abstractive.AbstractTransactionalQuery;
import com.antalex.db.service.api.ResultQuery;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

public class TransactionalSQLQuery extends AbstractTransactionalQuery {
    private PreparedStatement preparedStatement;

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
            preparedStatement.setClob(idx, ((Clob) o).getCharacterStream());
            return;
        }
        if (o instanceof URL) {
            preparedStatement.setURL(idx, (URL) o);
            return;
        }
        if (o instanceof LocalDateTime) {
            preparedStatement.setTimestamp(idx, Timestamp.valueOf((LocalDateTime) o));
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
        this.result = new ResultSQLQuery(this.preparedStatement.executeQuery());
        return result;
    }
}
