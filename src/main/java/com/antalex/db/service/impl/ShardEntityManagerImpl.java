package com.antalex.db.service.impl;

import com.antalex.db.entity.abstraction.ShardEntity;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.ShardEntityManager;
import com.antalex.db.service.ShardEntityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.GenericTypeResolver;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Primary
public class ShardEntityManagerImpl implements ShardEntityManager {
    private static final Map<Class<?>, ShardEntityRepository> REPOSITORIES = new HashMap<>();
    private ShardEntityRepository<?> currentShardEntityRepository;
    private Class<?> currentSourceClass;

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

    private synchronized <T extends ShardEntity> ShardEntityRepository<T> getEntityRepository(T entity) {
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
    public <T extends ShardEntity> ShardType getShardType(T entity) {
        if (Objects.isNull(entity)) {
            return null;
        }
        return getEntityRepository(entity).getShardType(entity);
    }

    @Override
    public <T extends ShardEntity> T save(T entity) {
        if (Objects.isNull(entity)) {
            return null;
        }
        return getEntityRepository(entity).save(entity);
    }

    @Override
    public <T extends ShardEntity> Iterable save(Iterable<T> entities) {
        if (Objects.nonNull(entities)) {
            entities.forEach(it -> it = save(it));
        }
        return entities;
    }
}
