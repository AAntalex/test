package com.antalex.db.service;

import com.antalex.db.entity.abstraction.ShardedEntity;

public interface ShardEntityRepository<T extends ShardedEntity> {
    T save(T entity);
}
