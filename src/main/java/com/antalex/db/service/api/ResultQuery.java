package com.antalex.db.service.api;

public interface ResultQuery {
    boolean next() throws Exception;
    Object getObject(int idx) throws Exception;
    long getLong(int idx) throws Exception;
    short getShort(int idx) throws Exception;
    boolean getBoolean(int idx) throws Exception;
    String getString(int idx) throws Exception;
}
