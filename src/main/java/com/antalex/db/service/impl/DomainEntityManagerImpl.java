package com.antalex.db.service.impl;

import com.antalex.db.domain.abstraction.Domain;
import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.exception.ShardDataBaseException;
import com.antalex.db.service.*;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.GenericTypeResolver;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Primary
@SuppressWarnings("unchecked")
public class DomainEntityManagerImpl implements DomainEntityManager {
    private static final Map<Class<?>, Mapper> MAPPERS = new HashMap<>();

    private ThreadLocal<Mapper> currentMapper = new ThreadLocal<>();
    private ThreadLocal<Class<?>> currentSourceClass = new ThreadLocal<>();

    @Autowired
    private ShardEntityManager entityManager;

    @Autowired
    public void setDomainMappers(
            List<DomainEntityMapper<? extends Domain, ? extends ShardInstance>> domainEntityMappers)
    {
        for (DomainEntityMapper<?, ?> domainEntityMapper : domainEntityMappers) {
            Class<?>[] classes = GenericTypeResolver
                    .resolveTypeArguments(domainEntityMapper.getClass(), DomainEntityMapper.class);
            if (Objects.nonNull(classes) && classes.length > 0) {
                MAPPERS.put(classes[0], new Mapper(domainEntityMapper, classes[1]));
            }
        }
    }

    private Mapper getMapper(Class<?> clazz) {
        Mapper mapper = currentMapper.get();
        if (mapper != null && currentSourceClass.get() == clazz) {
            return mapper;
        }
        mapper = Optional
                .ofNullable(MAPPERS.get(clazz.getSuperclass()))
                .orElse(MAPPERS.get(clazz));
        if (mapper == null) {
            throw new ShardDataBaseException(
                    String.format(
                            "Can't find shard DomainEntityMapper for class %s or superclass %s",
                            clazz.getName(),
                            clazz.getSuperclass().getName()
                    )
            );
        }
        currentMapper.set(mapper);
        currentSourceClass.set(clazz);
        return mapper;
    }

    @Override
    public <T extends Domain> T find(Class<T> clazz, Long id) {
        return getDomain(clazz, (ShardInstance) entityManager.find(getMapper(clazz).entityClass, id));
    }

    @Override
    public <T extends Domain> T newDomain(Class<T> clazz) {
        return (T) getMapper(clazz).domainEntityMapper.newDomain();
    }

    @Override
    public <T extends Domain> T getDomain(Class<T> clazz, ShardInstance entity) {
        return (T) getMapper(clazz).domainEntityMapper.getDomain(clazz, entity);
    }

    @AllArgsConstructor
    private class Mapper {
        DomainEntityMapper domainEntityMapper;
        Class entityClass;
    }
}
