package com.antalex.dao;

public interface ShardEntityRepository<T> {
    T save(T entity);
    <T> Iterable save(Iterable<T> entity);
}
