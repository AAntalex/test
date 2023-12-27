package com.antalex.db.entity.abstraction;

import com.antalex.db.model.StorageAttributes;
import com.antalex.db.model.enums.ShardType;

public interface ShardedEntity {
    Long getId();
    Long getOrderId();
    Long getShardValue();
    Short getClusterId();
    Short getShardId();
    Boolean isStored();
    void setStorageAttributes(Long id, Long shardValue, ShardType shardType);
    void setStorageAttributes(StorageAttributes storageAttributes);
}
