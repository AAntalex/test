package com.antalex.db.service.api;

import java.io.IOException;

public interface DataWrapper {
    void init(String content) throws IOException;
    void put(String attribute, Object o) throws IOException;
    <T> T get(String attribute, Class<T> clazz) throws IOException;
    String getContent();
}
