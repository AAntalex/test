package com.antalex.test;

public interface ShardEntityRepository2<T> {
    T convert(T entity);
}
