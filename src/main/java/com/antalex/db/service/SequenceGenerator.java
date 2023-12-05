package com.antalex.db.service;

public interface SequenceGenerator {
    long nextValue();
    long curValue();
    void init();
}
