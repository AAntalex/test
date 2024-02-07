package com.antalex.db.service.impl;

import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.StorageAttributes;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.ShardEntityManager;
import com.antalex.db.service.ShardEntityRepository;
import com.antalex.db.utils.ShardUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.GenericTypeResolver;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Primary
public class ShardEntityManagerImpl implements ShardEntityManager {
    private static final Map<Class<?>, ShardEntityRepository> REPOSITORIES = new HashMap<>();
    private ShardEntityRepository<?> currentShardEntityRepository;
    private Class<?> currentSourceClass;

    @Autowired
    private ShardDataBaseManager dataBaseManager;

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

    private synchronized <T extends ShardInstance> ShardEntityRepository<T> getEntityRepository(T entity) {
        if (entity == null) {
            return null;
        }
        if (currentShardEntityRepository != null && currentSourceClass == entity.getClass()) {
            return (ShardEntityRepository<T>) currentShardEntityRepository;
        }
        if (!REPOSITORIES.containsKey(entity.getClass())) {
            throw new IllegalStateException(
                    String.format("Can't find shard entity repository for %s", entity.getClass().getName())
            );
        }
        currentShardEntityRepository = REPOSITORIES.get(entity.getClass());
        currentSourceClass = entity.getClass();
        return (ShardEntityRepository<T>) currentShardEntityRepository;
    }

    @Override
    public <T extends ShardInstance> ShardType getShardType(T entity) {
        if (Objects.isNull(entity)) {
            return null;
        }
        return getEntityRepository(entity).getShardType(entity);
    }

    @Override
    public <T extends ShardInstance> Cluster getCluster(T entity) {
        if (Objects.isNull(entity)) {
            return null;
        }
        return getEntityRepository(entity).getCluster(entity);
    }

    @Override
    public <T extends ShardInstance> T save(T entity) {
        if (Objects.isNull(entity)) {
            return null;
        }
        return getEntityRepository(entity).save(entity);
    }

    @Override
    public <T extends ShardInstance> Iterable save(Iterable<T> entities) {
        if (Objects.nonNull(entities)) {
            entities.forEach(it -> it = save(it));
        }
        return entities;
    }

    @Override
    public <T extends ShardInstance> void setDependentStorage(T entity) {
        if (Objects.nonNull(entity)) {
            getEntityRepository(entity).setDependentStorage(entity);
        }
    }

    @Override
    public <T extends ShardInstance> void generateDependentId(T entity) {
        if (Objects.nonNull(entity)) {
            getEntityRepository(entity).generateDependentId(entity);
        }
    }

    @Override
    public <T extends ShardInstance> void setStorage(T entity, StorageAttributes storage, boolean isSave) {
        if (Objects.isNull(entity)) {
            return;
        }
        Cluster cluster = getCluster(entity);
        Optional.ofNullable(entity.getStorageAttributes())
                .map(entityStorage ->
                        Optional.ofNullable(storage)
                                .filter(it ->
                                        it != entityStorage &&
                                                getShardType(entity) != ShardType.REPLICABLE &&
                                                Objects.nonNull(entityStorage.getShard()) &&
                                                cluster.getId().equals(it.getCluster().getId())
                                )
                                .map(it ->
                                        Optional.ofNullable(storage.getShardValue())
                                                .map(shardValue -> {
                                                    storage.setShardValue(
                                                            ShardUtils.addShardValue(
                                                                    shardValue,
                                                                    entityStorage.getShardValue()
                                                            )
                                                    );
                                                    return null;
                                                })
                                                .orElseGet(() -> {
                                                    storage.setShard(entityStorage.getShard());
                                                    storage.setShardValue(entityStorage.getShardValue());
                                                    return null;
                                                })
                                )
                                .orElseGet(() -> {
                                    if (isSave) {
                                        setDependentStorage(entity);
                                    }
                                    return null;
                                })
                )
                .orElseGet(() -> {
                    entity.setStorageAttributes(
                            Optional.ofNullable(storage)
                                    .filter(it ->
                                            cluster.getId()
                                                    .equals(it.getCluster().getId())
                                    )
                                    .orElse(
                                            StorageAttributes.builder()
                                                    .stored(false)
                                                    .cluster(cluster)
                                                    .build()
                                    )
                    );
                    setDependentStorage(entity);
                    return null;
                });
    }

    @Override
    public <T extends ShardInstance> void setStorage(T entity, StorageAttributes storage) {
        setStorage(entity, storage, false);
    }

    @Override
    public <T extends ShardInstance> void setStorage(Iterable<T> entities, StorageAttributes storage) {
        entities.forEach(entity -> setStorage(entity, storage));
    }

    @Override
    public <T extends ShardInstance> void generateId(T entity, boolean isSave) {
        if (Objects.isNull(entity)) {
            return;
        }
        if (Objects.isNull(entity.getId())) {
            entity.setId(dataBaseManager.generateId(entity.getStorageAttributes()));
            generateDependentId(entity);
        } else {
            if (isSave) {
                generateDependentId(entity);
            }
        }
    }

    @Override
    public <T extends ShardInstance> void generateId(T entity) {
        generateId(entity, false);
    }

    @Override
    public <T extends ShardInstance> void generateId(Iterable<T> entities) {
        entities.forEach(this::generateId);
    }
}
