package com.antalex.test;

import com.antalex.db.entity.abstraction.ShardInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.GenericTypeResolver;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Primary
public class ShardEntityManager2 {
    private static final Map<Class<?>, ShardEntityRepository2> REPOSITORIES = new HashMap<>();
    private ShardEntityRepository2<?> currentShardEntityRepository;
    private Class<?> currentSourceClass;

    @Autowired
    public void setRepositories(List<ShardEntityRepository2> entityRepositories) {
        for (ShardEntityRepository2<?> shardEntityRepository : entityRepositories) {
            Class<?>[] classes = GenericTypeResolver
                    .resolveTypeArguments(shardEntityRepository.getClass(), ShardEntityRepository2.class);
            if (Objects.nonNull(classes) && classes.length > 0) {
                REPOSITORIES.putIfAbsent(classes[0], shardEntityRepository);
            }
        }
    }

    private synchronized <T extends ShardInstance> ShardEntityRepository2<T> getEntityRepository(T entity) {
        if (entity == null) {
            return null;
        }
        if (currentShardEntityRepository != null && currentSourceClass == entity.getClass()) {
            return (ShardEntityRepository2<T>) currentShardEntityRepository;
        }
        if (!REPOSITORIES.containsKey(entity.getClass())) {
            throw new IllegalStateException(
                    String.format("Can't find shard entity repository for %s", entity.getClass().getName())
            );
        }
        currentShardEntityRepository = REPOSITORIES.get(entity.getClass());
        currentSourceClass = entity.getClass();
        return (ShardEntityRepository2<T>) currentShardEntityRepository;
    }
}
