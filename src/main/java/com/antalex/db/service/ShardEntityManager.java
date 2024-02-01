package com.antalex.db.service;

import com.antalex.db.entity.abstraction.ShardEntity;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.StorageAttributes;
import com.antalex.db.model.enums.ShardType;

public interface ShardEntityManager {
    <T extends ShardEntity> ShardType getShardType(T entity);
    <T extends ShardEntity> Cluster getCluster(T entity);
    <T extends ShardEntity> T save(T entity);
    <T extends ShardEntity> Iterable save(Iterable<T> entities);
    <T extends ShardEntity> void setStorage(T entity, StorageAttributes storage);
}
