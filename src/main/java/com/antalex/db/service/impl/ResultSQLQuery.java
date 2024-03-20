package com.antalex.db.service.impl;

import com.antalex.db.service.api.ResultQuery;

import java.sql.ResultSet;
import java.sql.SQLException;

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
    public long getLong(int idx) throws SQLException {
        return result.getLong(idx);
    }

    @Override
    public Object getObject(int idx) throws SQLException {
        return result.getObject(idx);
    }

    @Override
    public boolean getBoolean(int idx) throws SQLException {
        return result.getBoolean(idx);
    }

    @Override
    public short getShort(int idx) throws SQLException {
        return result.getShort(idx);
    }

    @Override
    public String getString(int idx) throws SQLException {
        return result.getString(idx);
    }
}
