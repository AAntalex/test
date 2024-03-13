package com.antalex.db.service;

import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.enums.QueryType;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.api.TransactionalQuery;

import javax.persistence.EntityTransaction;
import java.util.UUID;

public interface ShardEntityManager {
    <T extends ShardInstance> ShardType getShardType(T entity);
    <T extends ShardInstance> Cluster getCluster(T entity);
    <T extends ShardInstance> T save(T entity);
    <T extends ShardInstance> Iterable saveAll(Iterable<T> entities);
    <T extends ShardInstance> void setDependentStorage(T entity);
    <T extends ShardInstance> void generateId(T entity, boolean force);
    <T extends ShardInstance> void generateId(T entity);
    <T extends ShardInstance> void persist(T entity);
    <T extends ShardInstance> void persistAll(Iterable<T> entities);
    <T extends ShardInstance> void generateAllId(Iterable<T> entities);
    <T extends ShardInstance> void generateDependentId(T entity);
    <T extends ShardInstance> void setStorage(T entity, ShardInstance parent, boolean force);
    <T extends ShardInstance> void setStorage(T entity, ShardInstance parent);
    <T extends ShardInstance> void setAllStorage(Iterable<T> entities, ShardInstance parent);
    <T extends ShardInstance> T newEntity(Class<T> clazz);
    <T extends ShardInstance> TransactionalQuery createQuery(T entity, String query, QueryType queryType);
    <T extends ShardInstance> Iterable<TransactionalQuery> createQueries(T entity, String query, QueryType queryType);
    <T extends ShardInstance> Iterable<TransactionalQuery> createNewShardsQueries(T entity, String query);
    <T extends ShardInstance> TransactionalQuery createQueryUnique(T entity, String query);
    <T extends ShardInstance> boolean lock(T entity);
    <T extends ShardInstance> T find(Class<T> clazz, Long id);
    EntityTransaction getTransaction();
    UUID getTransactionUUID();
    void setAutonomousTransaction();
    void addParallel();
    void flush();
}
