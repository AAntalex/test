package com.antalex.db.service.impl;

import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.StorageContext;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.ShardEntityRepository;
import com.antalex.db.service.api.ResultQuery;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ShardEntityRepositoryImpl<T extends ShardInstance> implements ShardEntityRepository<T> {
    @Override
    public T find(T entity) {
        return null;
    }

    @Override
    public List<T> findAll(String condition, Object... binds) {
        return null;
    }

    @Override
    public List<T> findAll(ShardInstance parent, String condition, Object... binds) {
        return null;
    }

    @Override
    public void extractValues(T entity, ResultQuery result, int index) {

    }

    @Override
    public void generateDependentId(T entity) {

    }

    @Override
    public ShardType getShardType() {
        return null;
    }

    @Override
    public Cluster getCluster() {
        return null;
    }

    @Override
    public T getEntity(Long id, StorageContext storageContext) {
        return null;
    }

    @Override
    public void setDependentStorage(T entity) {

    }

    @Override
    public T newEntity() {
        return null;
    }

    @Override
    public void persist(T entity, boolean onlyChanged) {

    }

    @Override
    public void lock(T entity) {

    }
}
