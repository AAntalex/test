package com.antalex.db.service;

import com.antalex.db.entity.abstraction.ShardedEntity;
import com.antalex.db.model.enums.ShardType;

public interface ShardEntityRepository<T extends ShardedEntity> {
    ShardType getShardType();
    T save(T entity);
}
