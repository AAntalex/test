package com.antalex.db.service;

import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.StorageContext;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.api.ResultQuery;

import java.util.List;

public interface ShardEntityRepository<T extends ShardInstance> {
    ShardType getShardType(T entity);
    Cluster getCluster(T entity);
    ShardType getShardType();
    Cluster getCluster();
    void generateDependentId(T entity);
    void setDependentStorage(T entity);
    T newEntity();
    T getEntity(Long id, StorageContext storageContext);
    void persist(T entity, boolean onlyChanged);
    void lock(T entity);
    T find(T entity);
    List<T> findAll(String condition, Object... binds);
    List<T> findAll(ShardInstance parent, String condition, Object... binds);
    void extractValues(T entity, ResultQuery result, int index);
}
