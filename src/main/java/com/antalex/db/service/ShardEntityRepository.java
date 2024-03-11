package com.antalex.db.service;

import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.enums.ShardType;

public interface ShardEntityRepository<T extends ShardInstance> {
    ShardType getShardType();
    Cluster getCluster();
    void generateDependentId(T entity);
    void setDependentStorage(T entity);
    T newEntity(Class<T> clazz);
    void persist(T entity);
    void lock(T entity);
}
