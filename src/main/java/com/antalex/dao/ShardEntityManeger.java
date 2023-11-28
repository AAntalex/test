package com.antalex.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.GenericTypeResolver;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Primary
public class ShardEntityManeger {
    private static final Map<Class<?>, ShardEntityRepository> REPOSITORIES = new HashMap<>();
    private ShardEntityRepository<?> currentShardEntityRepository;
    private Class<?> currentSourceClass;

    @Autowired
    public void setRepositories(List<ShardEntityRepository> entityRepositories) {
        for (ShardEntityRepository<?> shardEntityRepository : entityRepositories) {
            Class<?>[] classes = GenericTypeResolver
                    .resolveTypeArguments(shardEntityRepository.getClass(), ShardEntityRepository.class);
            REPOSITORIES.putIfAbsent(classes[0], shardEntityRepository);
        }
    }

    private <T> ShardEntityRepository<T> getEntityRepository(T entity) {
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

    public <T> T save(T entity) {
        if (entity == null) {
            return null;
        }
        return getEntityRepository(entity).save(entity);
    }

    public <T> Iterable save(Iterable<T> entities) {
        entities.forEach(it -> it = save(it));
        return entities;
    }
}
