package com.antalex.db.service.api;

public interface DataWrapper {
    void put(String attribute, Object o);
    <T> T get(String content, Class<T> clazz);
}
