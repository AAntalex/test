package com.antalex.db.service.api;

public interface EntityQuery {
    long nextValue();
    long curValue();
    void init();
}
