package com.antalex.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.GenericTypeResolver;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Primary
public class DtoMapper {
    private static final Map<Class<?>, Map<Class<?>, DtoConverter>> CONVERTERS = new HashMap<>();
    private DtoConverter<?, ?> currentConverter;
    private Class<?> currentTargetClass;
    private Class<?> currentSourceClass;

    @Autowired
    public void setConverters(List<DtoConverter> dtoConverters) {
        for (DtoConverter<?, ?> dtoConverter : dtoConverters) {
            Class<?>[] classes = GenericTypeResolver.resolveTypeArguments(dtoConverter.getClass(), DtoConverter.class);
            Map<Class<?>, DtoConverter> convertersMap;
            if (CONVERTERS.containsKey(classes[0])) {
                convertersMap = CONVERTERS.get(classes[0]);
            } else {
                convertersMap = new HashMap<>();
            }
            convertersMap.put(classes[1], dtoConverter);
            CONVERTERS.put(classes[0], convertersMap);
        }
    }

    private synchronized <T, M> DtoConverter<T, M> getDtoConverter(T entity, Class<M> targetClass) {
        if (entity == null) {
            return null;
        }
        if (currentConverter != null && currentSourceClass == entity.getClass() && currentTargetClass == targetClass) {
            return (DtoConverter<T, M>) currentConverter;
        }
        if (!CONVERTERS.containsKey(entity.getClass())) {
            throw new IllegalStateException(
                    String.format("Can't find converters for %s", entity.getClass().getName())
            );
        }
        Map<Class<?>, DtoConverter> convertersMap = CONVERTERS.get(entity.getClass());
        if (!convertersMap.containsKey(targetClass)) {
            throw new IllegalStateException(
                    String.format("Can't find converters from %s to %s", entity.getClass().getName(), targetClass.getName())
            );
        }
        currentConverter = convertersMap.get(targetClass);
        currentSourceClass = entity.getClass();
        currentTargetClass = targetClass;
        return (DtoConverter<T, M>) currentConverter;
    }

    public <T, M> M map(T entity, final Class<M> targetClass) {
        if (entity == null) {
            return null;
        }
        return getDtoConverter(entity, targetClass).convert(entity);
    }

    public <T, M, K> M map(Map.Entry<K, T> entity, final Class<M> targetClass) {
        if (entity == null || entity.getValue() == null) {
            return null;
        }
        return getDtoConverter(entity.getValue(), targetClass).convert(entity.getValue(), entity.getKey());
    }

    public <T, M> Stream<M> mapStream(final Stream<T> entities, final Class<M> targetClass) {
        return entities.map(entity -> map(entity, targetClass));
    }

    public <T, M> List<M> map(final Stream<T> entities, final Class<M> targetClass) {
        return mapStream(entities, targetClass).collect(Collectors.toList());
    }

    public <T, M> List<M> map(final List<T> entities, final Class<M> targetClass) {
        return map(entities.stream(), targetClass);
    }

    public <T, M, K> Stream<M> mapEntryStream(final Stream<Map.Entry<K, T>> entities, final Class<M> targetClass) {
        return entities.map(entity -> map(entity, targetClass));
    }

    public <T, M, K> List<M> mapToList(final Stream<Map.Entry<K, T>> entities, final Class<M> targetClass) {
         return mapEntryStream(entities, targetClass).collect(Collectors.toList());
    }

    public <T, M, K> List<M> mapToList(final Map<K, T> entities, final Class<M> targetClass) {
        return mapToList(entities.entrySet().stream(), targetClass);
    }

}
