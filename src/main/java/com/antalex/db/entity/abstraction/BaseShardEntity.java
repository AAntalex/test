package com.antalex.db.entity.abstraction;

import com.antalex.db.model.StorageAttributes;
import com.antalex.db.utils.ShardUtils;

import java.util.Optional;

public abstract class BaseShardEntity implements ShardInstance {
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
    public StorageAttributes getStorageAttributes() {
        return this.storageAttributes;
    }

    @Override
    public void setStorageAttributes(StorageAttributes storageAttributes) {
        this.storageAttributes = storageAttributes;
    }
}
