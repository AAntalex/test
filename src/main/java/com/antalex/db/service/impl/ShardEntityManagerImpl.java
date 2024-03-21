package com.antalex.db.service.impl;

import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.exception.ShardDataBaseException;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.Shard;
import com.antalex.db.model.StorageContext;
import com.antalex.db.model.enums.QueryStrategy;
import com.antalex.db.model.enums.QueryType;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.ShardEntityManager;
import com.antalex.db.service.ShardEntityRepository;
import com.antalex.db.service.SharedTransactionManager;
import com.antalex.db.service.api.TransactionalQuery;
import com.antalex.db.utils.ShardUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.GenericTypeResolver;
import org.springframework.stereotype.Component;

import javax.persistence.EntityTransaction;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Primary
public class ShardEntityManagerImpl implements ShardEntityManager {
    private static final Map<Class<?>, ShardEntityRepository> REPOSITORIES = new HashMap<>();

    private ThreadLocal<ShardEntityRepository<?>> currentShardEntityRepository = new ThreadLocal<>();
    private ThreadLocal<Class<?>> currentSourceClass = new ThreadLocal<>();

    @Autowired
    private ShardDataBaseManager dataBaseManager;
    @Autowired
    private SharedTransactionManager sharedTransactionManager;

    @Autowired
    public void setRepositories(List<ShardEntityRepository> entityRepositories) {
        for (ShardEntityRepository<?> shardEntityRepository : entityRepositories) {
            Class<?>[] classes = GenericTypeResolver
                    .resolveTypeArguments(shardEntityRepository.getClass(), ShardEntityRepository.class);
            if (Objects.nonNull(classes) && classes.length > 0) {
                REPOSITORIES.putIfAbsent(classes[0], shardEntityRepository);
            }
        }
    }

    private <T extends ShardInstance> ShardEntityRepository<T> getEntityRepository(Class<?> clazz) {
        ShardEntityRepository repository = currentShardEntityRepository.get();
        if (repository != null && currentSourceClass.get() == clazz) {
            return repository;
        }
        repository = Optional
                .ofNullable(REPOSITORIES.get(clazz.getSuperclass()))
                .orElse(REPOSITORIES.get(clazz));
        if (repository == null) {
            throw new ShardDataBaseException(
                    String.format(
                            "Can't find shard entity repository for class %s or superclass %s",
                            clazz.getName(),
                            clazz.getSuperclass().getName()
                    )
            );
        }
        currentShardEntityRepository.set(repository);
        currentSourceClass.set(clazz);
        return repository;
    }

    @Override
    public <T extends ShardInstance> ShardType getShardType(T entity) {
        if (entity == null) {
            return null;
        }
        return getEntityRepository(entity.getClass()).getShardType();
    }

    @Override
    public <T extends ShardInstance> Cluster getCluster(T entity) {
        if (entity == null) {
            return null;
        }
        return getEntityRepository(entity.getClass()).getCluster();
    }

    @Override
    public <T extends ShardInstance> T save(T entity) {
        setStorage(entity, null, true);
        generateId(entity, true);
        boolean isAurTransaction = startTransaction();
        persist(entity, true);
        if (isAurTransaction) {
            flush();
        }
        return entity;
    }

    @Override
    public <T extends ShardInstance> Iterable saveAll(Iterable<T> entities) {
        if (entities == null) {
            return null;
        }
        boolean isAurTransaction = startTransaction();
        entities.forEach(it -> it = save(it));
        if (isAurTransaction) {
            flush();
        }
        return entities;
    }

    @Override
    public <T extends ShardInstance> void setDependentStorage(T entity) {
        if (entity == null) {
            return;
        }
        getEntityRepository(entity.getClass()).setDependentStorage(entity);
    }

    @Override
    public <T extends ShardInstance> boolean lock(T entity) {
        if (entity == null) {
            return false;
        }
        try {
            getEntityRepository(entity.getClass()).lock(entity);
            return true;
        } catch (ShardDataBaseException err) {
            return false;
        }
    }

    @Override
    public <T extends ShardInstance> void generateDependentId(T entity) {
        if (entity == null) {
            return;
        }
        getEntityRepository(entity.getClass()).generateDependentId(entity);
    }

    @Override
    public <T extends ShardInstance> void setStorage(T entity, ShardInstance parent, boolean force) {
        if (entity == null) {
            return;
        }
        Cluster cluster = getCluster(entity);
        ShardType shardType = getShardType(entity);
        if (
                Optional.ofNullable(entity.getStorageContext())
                        .map(entityStorage ->
                                Optional.ofNullable(parent)
                                        .map(ShardInstance::getStorageContext)
                                        .filter(it ->
                                                it != entityStorage &&
                                                        shardType != ShardType.REPLICABLE &&
                                                        Objects.nonNull(entityStorage.getShard()) &&
                                                        cluster.getId().equals(it.getCluster().getId()) &&
                                                        dataBaseManager.isEnabled(it.getShard())
                                        )
                                        .map(storage ->
                                                Optional.ofNullable(storage.getShard())
                                                        .map(shard -> {
                                                            storage.setShardMap(
                                                                    ShardUtils.addShardMap(
                                                                            ShardUtils.getShardMap(shard.getId()),
                                                                            entityStorage.getShardMap()
                                                                    )
                                                            );
                                                            return false;
                                                        })
                                                        .orElseGet(() -> {
                                                            storage.setShard(entityStorage.getShard());
                                                            storage.setShardMap(
                                                                    ShardUtils.getShardMap(
                                                                            entityStorage.getShard().getId()
                                                                    )
                                                            );
                                                            return true;
                                                        })
                                        )
                                        .orElseGet(() -> {
                                            if (force) {
                                                setDependentStorage(entity);
                                            }
                                            return false;
                                        })
                        )
                        .orElseGet(() -> {
                            entity.setStorageContext(
                                    Optional.ofNullable(parent)
                                            .map(ShardInstance::getStorageContext)
                                            .filter(it ->
                                                    cluster.getId()
                                                            .equals(it.getCluster().getId()) &&
                                                            shardType != ShardType.REPLICABLE &&
                                                            dataBaseManager.isEnabled(it.getShard())
                                            )
                                            .map(storage ->
                                                    Optional.ofNullable(storage.getShard())
                                                            .map(shard ->
                                                                    StorageContext.builder()
                                                                            .cluster(cluster)
                                                                            .stored(false)
                                                                            .shard(shard)
                                                                            .shardMap(
                                                                                    ShardUtils.getShardMap(
                                                                                            shard.getId()
                                                                                    )
                                                                            )
                                                                            .build()
                                                            )
                                                            .orElse(storage)
                                            )
                                            .orElse(
                                                    StorageContext.builder()
                                                            .cluster(cluster)
                                                            .temporary(true)
                                                            .stored(false)
                                                            .build()
                                            )
                            );
                            setDependentStorage(entity);
                            return Optional.ofNullable(parent)
                                    .map(ShardInstance::getStorageContext)
                                    .map(StorageContext::isTemporary)
                                    .orElse(false) &&
                                    Objects.nonNull(entity.getStorageContext().getShard());
                        }))
        {
            parent.setStorageContext(
                    StorageContext.builder()
                            .cluster(parent.getStorageContext().getCluster())
                            .shard(parent.getStorageContext().getShard())
                            .shardMap(parent.getStorageContext().getShardMap())
                            .stored(false)
                            .build()
            );
        }
    }

    @Override
    public <T extends ShardInstance> void setStorage(T entity, ShardInstance parent) {
        setStorage(entity, parent, false);
    }

    @Override
    public <T extends ShardInstance> void setAllStorage(Iterable<T> entities, ShardInstance parent) {
        if (entities == null) {
            return;
        }
        entities.forEach(entity -> setStorage(entity, parent));
    }

    @Override
    public <T extends ShardInstance> void generateId(T entity, boolean force) {
        if (entity == null) {
            return;
        }
        if (Objects.isNull(entity.getId())) {
            dataBaseManager.generateId(entity);
            generateDependentId(entity);
        } else {
            if (force) {
                generateDependentId(entity);
            }
        }
    }

    @Override
    public <T extends ShardInstance> void generateId(T entity) {
        generateId(entity, false);
    }

    private <T extends ShardInstance> void persist(T entity, boolean force) {
        if (entity == null) {
            return;
        }
        if (entity.setTransactionalContext(getTransaction())) {
            ShardEntityRepository<T> repository = getEntityRepository(entity.getClass());
            checkShardMap(entity, repository.getShardType());
            if (force || !entity.isStored() || entity.isChanged() || entity.hasNewShards())
            {
                repository.persist(entity);
                entity.getStorageContext().persist();
            }
        }
    }

    @Override
    public <T extends ShardInstance> void persist(T entity) {
        persist(entity, false);
    }

    @Override
    public <T extends ShardInstance> void persistAll(Iterable<T> entities) {
        if (entities == null) {
            return;
        }
        entities.forEach(it -> persist(it, false));
    }

    @Override
    public <T extends ShardInstance> void generateAllId(Iterable<T> entities) {
        if (entities == null) {
            return;
        }
        entities.forEach(this::generateId);
    }

    @Override
    public <T extends ShardInstance> T newEntity(Class<T> clazz) {
        ShardEntityRepository<T> repository = getEntityRepository(clazz);
        return repository.newEntity();
    }

    @Override
    public <T extends ShardInstance> T newEntity(Class<T> clazz, Long id) {
        ShardEntityRepository<T> repository = getEntityRepository(clazz);
        return repository.newEntity(id, dataBaseManager.getStorageContext(id));
    }

    @Override
    public EntityTransaction getTransaction() {
        return sharedTransactionManager.getTransaction();
    }

    @Override
    public String getTransactionUUID() {
        return sharedTransactionManager.getTransactionUUID().toString();
    }

    @Override
    public void setAutonomousTransaction() {
        sharedTransactionManager.setAutonomousTransaction();
    }

    @Override
    public void flush() {
        getTransaction().commit();
    }

    @Override
    public void addParallel() {
        ((SharedEntityTransaction) getTransaction()).addParallel();
    }

    @Override
    public <T extends ShardInstance> TransactionalQuery createQuery(
            T entity,
            String query,
            QueryType queryType,
            QueryStrategy queryStrategy)
    {
        switch (queryStrategy) {
            case OWN_SHARD:
                return this.createQuery(entity.getStorageContext().getShard(), query, queryType);
            case MAIN_SHARD:
                return this.createQuery(entity.getStorageContext().getCluster().getMainShard(), query, queryType);
            case ALL_SHARDS:
            case NEW_SHARDS:
                return getMainQuery(
                        createQueries(entity, query, queryType, queryStrategy)
                );
        }
        return null;
    }

    @Override
    public <T extends ShardInstance> Iterable<TransactionalQuery> createQueries(
            T entity,
            String query,
            QueryType queryType,
            QueryStrategy queryStrategy)
    {
        switch (queryStrategy) {
            case OWN_SHARD:
            case MAIN_SHARD:
                return Collections.singletonList(createQuery(entity, query, queryType, queryStrategy));
            case ALL_SHARDS:
                return dataBaseManager.getEntityShards(entity)
                        .map(shard -> this.createQuery(shard, query, queryType))
                        .collect(Collectors.toList());
            case NEW_SHARDS:
                return dataBaseManager.getNewShards(entity)
                        .map(shard -> this.createQuery(shard, query, queryType))
                        .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public <T extends ShardInstance> Iterable<TransactionalQuery> createQueries(
            Class<T> clazz,
            String query,
            QueryType queryType)
    {
        ShardEntityRepository<T> repository = getEntityRepository(clazz);
        return dataBaseManager.getEnabledShards(repository.getCluster())
                .map(shard -> this.createQuery(shard, query, queryType))
                .collect(Collectors.toList());
    }

    @Override
    public <T extends ShardInstance> TransactionalQuery createQuery(Class<T> clazz, String query, QueryType queryType) {
        return getMainQuery(createQueries(clazz, query, queryType));
    }

    @Override
    public <T extends ShardInstance> T find(Class<T> clazz, Long id) {
        ShardEntityRepository<T> repository = getEntityRepository(clazz);
        return repository.find(id, dataBaseManager.getStorageContext(id));
    }

    @Override
    public <T extends ShardInstance> T find(T entity) {
        if (entity == null) {
            return null;
        }
        ShardEntityRepository<T> repository = getEntityRepository(entity.getClass());
        return repository.find(entity);
    }

    private TransactionalQuery getMainQuery(Iterable<TransactionalQuery> queries) {




        TransactionalQuery mainQuery = null;
        for (TransactionalQuery query : queries) {
            if (Objects.isNull(mainQuery)) {
                mainQuery = query;
            } else {
                mainQuery.addRelatedQuery(query);
            }
        }
        return mainQuery;
    }

    private TransactionalQuery createQuery(Shard shard, String query, QueryType queryType) {
        TransactionalQuery transactionalQuery = dataBaseManager.getTransactionalTask(shard).addQuery(query, queryType);
        transactionalQuery.setShard(shard);
        if (queryType == QueryType.SELECT) {
            transactionalQuery.setParallelRun(((SharedEntityTransaction) getTransaction()).getParallelRun());
        }
        return transactionalQuery;
    }

    private boolean startTransaction() {
        if (!getTransaction().isActive()) {
            getTransaction().begin();
            return true;
        }
        return false;
    }

    private void checkShardMap(ShardInstance entity, ShardType shardType) {
        Long shardMap = entity.getStorageContext().getShardMap();
        if (shardType == ShardType.REPLICABLE && !shardMap.equals(0L))
        {
            entity.getStorageContext().setShardMap(0L);
        }
        if (shardType == ShardType.SHARDABLE
                && !shardMap.equals(ShardUtils.getShardMap(entity.getStorageContext().getShard().getId())))
        {
            throw new ShardDataBaseException("У шардируемой сущности не может быть определенно более 1 шарды.");
        }
    }
}
