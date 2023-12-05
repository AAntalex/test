package com.antalex.db.service;

import com.antalex.db.entity.abstraction.ShardedEntity;

public interface ShardEntityManager {
    <T extends ShardedEntity> T save(T entity);
    <T extends ShardedEntity> Iterable save(Iterable<T> entities);
}
