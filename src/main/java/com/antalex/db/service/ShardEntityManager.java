package com.antalex.db.service;

import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.Shard;
import com.antalex.db.model.StorageAttributes;
import com.antalex.db.model.enums.ShardType;

import javax.persistence.EntityTransaction;

public interface ShardEntityManager {
    <T extends ShardInstance> ShardType getShardType(T entity);
    <T extends ShardInstance> Cluster getCluster(T entity);
    <T extends ShardInstance> T save(T entity);
    <T extends ShardInstance> Iterable saveAll(Iterable<T> entities);
    <T extends ShardInstance> void setDependentStorage(T entity);
    <T extends ShardInstance> void generateId(T entity, boolean isSave);
    <T extends ShardInstance> void generateId(T entity);
    <T extends ShardInstance> void generateAllId(Iterable<T> entities);
    <T extends ShardInstance> void generateDependentId(T entity);
    <T extends ShardInstance> void setStorage(T entity, StorageAttributes storage, boolean isSave);
    <T extends ShardInstance> void setStorage(T entity, StorageAttributes storage);
    <T extends ShardInstance> void setAllStorage(Iterable<T> entities, StorageAttributes storage);
    <T extends ShardInstance> T newEntity(Class<T> clazz);
    EntityTransaction getTransaction();
    void setAutonomousTransaction();
    void flush();
}
