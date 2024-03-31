package com.antalex.db.service;

import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.StorageContext;
import com.antalex.db.model.enums.ShardType;

import java.util.List;
import java.util.Objects;

public interface ShardEntityRepository<T extends ShardInstance> {
    ShardType getShardType();
    Cluster getCluster();
    void generateDependentId(T entity);
    void setDependentStorage(T entity);
    T newEntity();
    T newEntity(Long id, StorageContext storageContext);
    void persist(T entity, boolean onlyChanged);
    void lock(T entity);
    T find(T entity);
    List<T> findAll(String condition, Object... binds);
}
