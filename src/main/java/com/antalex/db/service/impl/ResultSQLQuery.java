package com.antalex.db.service.impl;

import com.antalex.db.service.api.ResultQuery;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Date;

public class ResultSQLQuery implements ResultQuery {
    private ResultSet result;

    ResultSQLQuery(ResultSet result) {
        this.result = result;
    }

    @Override
    public boolean next() throws SQLException {
        return result.next();
    }

    @Override
    public Long getLong(int idx) throws SQLException {
        Long ret = result.getLong(idx);
        return result.wasNull() ? null : ret;
    }

    @Override
    public Object getObject(int idx) throws SQLException {
        return result.getObject(idx);
    }

    @Override
    public Boolean getBoolean(int idx) throws SQLException {
        Boolean ret = result.getBoolean(idx);
        return result.wasNull() ? null : ret;
    }

    @Override
    public Short getShort(int idx) throws SQLException {
        Short ret = result.getShort(idx);
        return result.wasNull() ? null : ret;
    }

    @Override
    public String getString(int idx) throws SQLException {
        return result.getString(idx);
    }

    @Override
    public BigDecimal getBigDecimal(int idx) throws SQLException {
        BigDecimal ret = result.getBigDecimal(idx);
        return result.wasNull() ? null : ret;
    }

    @Override
    public Byte getByte(int idx) throws SQLException {
        Byte ret = result.getByte(idx);
        return result.wasNull() ? null : ret;
    }

    @Override
    public Double getDouble(int idx) throws SQLException {
        Double ret = result.getDouble(idx);
        return result.wasNull() ? null : ret;
    }

    @Override
    public Float getFloat(int idx) throws SQLException {
        Float ret = result.getFloat(idx);
        return result.wasNull() ? null : ret;
    }

    @Override
    public Integer getInteger(int idx) throws SQLException {
        Integer ret = result.getInt(idx);
        return result.wasNull() ? null : ret;
    }

    @Override
    public Date getDate(int idx) throws SQLException {
        return result.getDate(idx);
    }

    @Override
    public Time getTime(int idx) throws SQLException {
        return result.getTime(idx);
    }

    @Override
    public Timestamp getTimestamp(int idx) throws SQLException {
        return result.getTimestamp(idx);
    }

    @Override
    public Blob getBlob(int idx) throws SQLException {
        return result.getBlob(idx);
    }

    @Override
    public Clob getClob(int idx) throws SQLException {
        return result.getClob(idx);
    }

    @Override
    public RowId getRowId(int idx) throws SQLException {
        return result.getRowId(idx);
    }

    @Override
    public URL getURL(int idx) throws SQLException {
        return result.getURL(idx);
    }

    @Override
    public SQLXML getSQLXML(int idx) throws SQLException {
        return result.getSQLXML(idx);
    }
}
