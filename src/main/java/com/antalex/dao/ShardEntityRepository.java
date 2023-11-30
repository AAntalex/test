package com.antalex.dao;

public interface ShardEntityRepository<T> {
    T save(T entity);
}
