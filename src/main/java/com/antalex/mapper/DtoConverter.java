package com.antalex.mapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface DtoConverter<T, M> {
    M convert(T entity);

    default <K> M convert(T entity, K key) {
        return this.convert(entity);
    }

    default Stream<M> convertStream(Stream<T> stream) {
        if (stream == null) {
            return Stream.empty();
        }
        return stream.map(this::convert);
    }

    default List<M> convert(Stream<T> stream) {
        if (stream == null) {
            return Collections.emptyList();
        }
        return convertStream(stream).collect(Collectors.toList());
    }

    default List<M> convert(List<T> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        return convert(entities.stream());
    }

    default <K> Stream<M> convertToListStream(Stream<Map.Entry<K, T>> stream) {
        if (stream == null) {
            return Stream.empty();
        }
        return stream.map(entry -> this.convert(entry.getValue(), entry.getKey()));
    }

    default <K> List<M> convertToList(Stream<Map.Entry<K, T>> stream) {
        if (stream == null) {
            return Collections.emptyList();
        }
        return convertToListStream(stream).collect(Collectors.toList());
    }

    default <K> List<M> convert(Map<K, T> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        return convertToList(entities.entrySet().stream());
    }
}
