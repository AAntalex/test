package com.antalex.db.service;

import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.enums.ShardType;

public interface ShardEntityRepository<T extends ShardInstance> {
    ShardType getShardType(T entity);
    Cluster getCluster(T entity);
    T save(T entity);
    void setDependentStorage(T entity);
}
