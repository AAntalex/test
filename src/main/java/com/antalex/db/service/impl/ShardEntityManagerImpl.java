package com.antalex.db.service.impl;

import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.Shard;
import com.antalex.db.model.StorageAttributes;
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
            throw new IllegalStateException(
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
        return getEntityRepository(entity.getClass()).getShardType(entity);
    }

    @Override
    public <T extends ShardInstance> Cluster getCluster(T entity) {
        if (entity == null) {
            return null;
        }
        return getEntityRepository(entity.getClass()).getCluster(entity);
    }

    @Override
    public <T extends ShardInstance> T save(T entity) {
        setStorage(entity, null, true);
        generateId(entity, true);
        boolean isAurTransaction = startTransaction();
        persist(entity);
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
                Optional.ofNullable(entity.getStorageAttributes())
                        .map(entityStorage ->
                                Optional.ofNullable(parent)
                                        .map(ShardInstance::getStorageAttributes)
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
                                                            storage.setShardValue(
                                                                    ShardUtils.addShardValue(
                                                                            ShardUtils.getShardValue(shard.getId()),
                                                                            entityStorage.getShardValue()
                                                                    )
                                                            );
                                                            return false;
                                                        })
                                                        .orElseGet(() -> {
                                                            storage.setShard(entityStorage.getShard());
                                                            storage.setShardValue(
                                                                    ShardUtils.getShardValue(
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
                            entity.setStorageAttributes(
                                    Optional.ofNullable(parent)
                                            .map(ShardInstance::getStorageAttributes)
                                            .filter(it ->
                                                    cluster.getId()
                                                            .equals(it.getCluster().getId()) &&
                                                            shardType != ShardType.REPLICABLE &&
                                                            dataBaseManager.isEnabled(it.getShard())
                                            )
                                            .map(storage ->
                                                    Optional.ofNullable(storage.getShard())
                                                            .map(shard ->
                                                                    StorageAttributes.builder()
                                                                            .cluster(cluster)
                                                                            .stored(false)
                                                                            .shard(shard)
                                                                            .shardValue(
                                                                                    ShardUtils.getShardValue(
                                                                                            shard.getId()
                                                                                    )
                                                                            )
                                                                            .build()
                                                            )
                                                            .orElse(storage)
                                            )
                                            .orElse(
                                                    StorageAttributes.builder()
                                                            .cluster(cluster)
                                                            .temporary(true)
                                                            .stored(false)
                                                            .build()
                                            )
                            );
                            setDependentStorage(entity);
                            return Optional.ofNullable(parent)
                                    .map(ShardInstance::getStorageAttributes)
                                    .map(StorageAttributes::getTemporary)
                                    .orElse(false) &&
                                    Objects.nonNull(entity.getStorageAttributes().getShard());
                        }))
        {
            parent.setStorageAttributes(
                    StorageAttributes.builder()
                            .cluster(parent.getStorageAttributes().getCluster())
                            .shard(parent.getStorageAttributes().getShard())
                            .shardValue(parent.getStorageAttributes().getShardValue())
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

    @Override
    public <T extends ShardInstance> void persist(T entity) {
        if (entity == null) {
            return;
        }
        getEntityRepository(entity.getClass()).persist(entity);
    }

    @Override
    public <T extends ShardInstance> void persistAll(Iterable<T> entities) {
        if (entities == null) {
            return;
        }
        entities.forEach(this::persist);
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
        return repository.newEntity(clazz);
    }

    @Override
    public EntityTransaction getTransaction() {
        return sharedTransactionManager.getTransaction();
    }

    @Override
    public UUID getTransactionUUID() {
        return sharedTransactionManager.getTransactionUUID();
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
    public <T extends ShardInstance> TransactionalQuery createQuery(T entity, String query, QueryType queryType) {
        Shard shard = entity.getStorageAttributes().getShard();
        if (
                queryType == QueryType.DML &&
                        !entity.getStorageAttributes().getShardValue().equals(ShardUtils.getShardValue(shard.getId())))
        {
            throw new IllegalStateException(
                    "Для реплицируемых или мульти-шардовых сущностей" +
                            " слеует использовать метод createQueries вместо createQuery!"
            );
        }
        return this.createQuery(shard, query, queryType);
    }

    @Override
    public <T extends ShardInstance> Iterable<TransactionalQuery> createQueries(
            T entity,
            String query,
            QueryType queryType)
    {
        return dataBaseManager.getAllShards(entity)
                .map(shard -> this.createQuery(shard, query, queryType))
                .collect(Collectors.toList());
    }

    @Override
    public <T extends ShardInstance> Iterable<TransactionalQuery> createNewQueries(T entity, String query) {
        return dataBaseManager.getNewShards(entity)
                .map(shard -> this.createQuery(shard, query, QueryType.DML))
                .collect(Collectors.toList());
    }

    private TransactionalQuery createQuery(Shard shard, String query, QueryType queryType) {
        return dataBaseManager.getTransactionalTask(shard).addQuery(query, queryType);
    }

    private boolean startTransaction() {
        if (!getTransaction().isActive()) {
            getTransaction().begin();
            return true;
        }
        return false;
    }
}
