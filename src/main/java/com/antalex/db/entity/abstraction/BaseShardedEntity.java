package com.antalex.db.entity.abstraction;

import com.antalex.db.model.Cluster;
import com.antalex.db.model.Shard;
import com.antalex.db.model.StorageAttributes;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.utils.ShardUtils;

import java.util.Objects;
import java.util.Optional;

public abstract class BaseShardedEntity implements ShardedEntity {
    private Long id;
    private StorageAttributes storageAttributes;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getOrderId() {
        return Optional.ofNullable(this.id)
                .map(it -> it % (ShardUtils.MAX_SHARDS * ShardUtils.MAX_CLUSTERS))
                .orElse(null);
    }

    @Override
    public Shard getShard() {
        return Optional.ofNullable(storageAttributes)
                .map(StorageAttributes::getShard)
                .orElse(null);
    }

    @Override
    public Cluster getCluster() {
        return Optional.ofNullable(storageAttributes)
                .map(StorageAttributes::getCluster)
                .orElse(null);
    }

    @Override
    public Long getShardValue() {
        return Optional.ofNullable(storageAttributes)
                .map(StorageAttributes::getShardValue)
                .orElse(null);
    }

    @Override
    public ShardType getShardType() {
        return Optional.ofNullable(storageAttributes)
                .map(StorageAttributes::getShardType)
                .orElse(null);
    }

    @Override
    public Boolean isStored() {
        return Optional.ofNullable(storageAttributes)
                .map(StorageAttributes::getStored)
                .orElse(false);
    }

    @Override
    public void setStorageAttributes(StorageAttributes storageAttributes) {
        this.storageAttributes = storageAttributes;
    }
}
