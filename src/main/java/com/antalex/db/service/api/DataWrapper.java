package com.antalex.db.service.api;

public interface DataWrapper {
    void init(String content) throws Exception;
    void put(String attribute, Object o);
    <T> T get(String attribute, Class<T> clazz) throws Exception;
    String getContent();
}
