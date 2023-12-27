package com.antalex.db.service;

import com.antalex.db.entity.abstraction.ShardedEntity;
import com.antalex.db.model.enums.ShardType;

public interface ShardEntityManager {
    <T extends ShardedEntity> ShardType getShardType(T entity);
    <T extends ShardedEntity> T save(T entity);
    <T extends ShardedEntity> Iterable save(Iterable<T> entities);
}
