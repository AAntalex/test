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

import javax.persistence.EntityTransaction;
import java.util.*;

@Component
@Primary
public class ShardEntityManagerImpl implements ShardEntityManager {
    private static final Map<Class<?>, ShardEntityRepository> REPOSITORIES = new HashMap<>();

    private ThreadLocal<ShardEntityRepository<?>> currentShardEntityRepository = new ThreadLocal<>();
    private ThreadLocal<Class<?>> currentSourceClass = new ThreadLocal<>();

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
        if (entity == null) {
            return null;
        }
        ShardEntityRepository<T> repository = getEntityRepository(entity.getClass());
        return repository.save(entity);
    }

    @Override
    public <T extends ShardInstance> Iterable saveAll(Iterable<T> entities) {
        if (entities == null) {
            return null;
        }
        entities.forEach(it -> it = save(it));
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
    public <T extends ShardInstance> void setStorage(T entity, StorageAttributes storage, boolean isSave) {
        if (entity == null) {
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
                                                    return storage;
                                                })
                                                .orElseGet(() -> {
                                                    storage.setShard(entityStorage.getShard());
                                                    storage.setShardValue(entityStorage.getShardValue());
                                                    return storage;
                                                })
                                )
                                .orElseGet(() -> {
                                    if (isSave) {
                                        setDependentStorage(entity);
                                    }
                                    return entity.getStorageAttributes();
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
                    return entity.getStorageAttributes();
                });
    }

    @Override
    public <T extends ShardInstance> void setStorage(T entity, StorageAttributes storage) {
        setStorage(entity, storage, false);
    }

    @Override
    public <T extends ShardInstance> void setAllStorage(Iterable<T> entities, StorageAttributes storage) {
        if (entities == null) {
            return;
        }
        entities.forEach(entity -> setStorage(entity, storage));
    }

    @Override
    public <T extends ShardInstance> void generateId(T entity, boolean isSave) {
        if (entity == null) {
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
        return null;
    }
}
