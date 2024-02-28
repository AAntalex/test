package com.antalex.service;

import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.StorageAttributes;
import com.antalex.db.model.enums.ShardType;

public interface TestEntityManager {
    <T extends ShardInstance> void generateId(T entity);
    <T extends ShardInstance> void setStorage(T entity, ShardInstance parent);
}
