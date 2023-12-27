package com.antalex.db.entity.abstraction;

import com.antalex.db.model.StorageAttributes;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.utils.ShardUtils;

import java.util.Objects;
import java.util.Optional;

public abstract class BaseShardedEntity implements ShardedEntity {
    private StorageAttributes storageAttributes;

    @Override
    public Long getId() {
        return Optional.ofNullable(storageAttributes)
                .map(StorageAttributes::getId)
                .orElse(null);
    }

    @Override
    public Long getOrderId() {
        return Optional.ofNullable(this.storageAttributes)
                .map(StorageAttributes::getId)
                .map(it -> it % (ShardUtils.MAX_SHARDS * ShardUtils.MAX_CLUSTERS))
                .orElse(null);
    }

    @Override
    public Short getShardId() {
        return Optional.ofNullable(storageAttributes)
                .map(StorageAttributes::getShard)
                .orElse(null);
    }

    @Override
    public Short getClusterId() {
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
    public Boolean isStored() {
        return Optional.ofNullable(storageAttributes)
                .map(StorageAttributes::getStored)
                .orElse(false);
    }

    @Override
    public void setStorageAttributes(Long id, Long shardValue, ShardType shardType) {
        if (Objects.nonNull(id)) {
            if (Objects.isNull(storageAttributes)) {
                storageAttributes = new StorageAttributes();
            }
            storageAttributes.setStored(true);
            storageAttributes.setId(id);
            storageAttributes.setShard((short) (id % ShardUtils.MAX_SHARDS));
            storageAttributes.setCluster((short) (id / ShardUtils.MAX_SHARDS % ShardUtils.MAX_CLUSTERS));
            storageAttributes.setShardValue(shardValue);
            storageAttributes.setShardType(shardType);
        }
    }

    @Override
    public void setStorageAttributes(StorageAttributes storageAttributes) {
        this.storageAttributes = storageAttributes;
    }
}
