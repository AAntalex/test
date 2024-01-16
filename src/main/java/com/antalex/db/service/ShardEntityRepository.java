package com.antalex.db.service;

import com.antalex.db.entity.abstraction.ShardEntity;
import com.antalex.db.model.enums.ShardType;

public interface ShardEntityRepository<T extends ShardEntity> {
    ShardType getShardType(T entity);
    T save(T entity);
}
