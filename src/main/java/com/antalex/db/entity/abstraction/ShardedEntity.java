package com.antalex.db.entity.abstraction;

import com.antalex.db.model.Cluster;
import com.antalex.db.model.Shard;
import com.antalex.db.model.StorageAttributes;
import com.antalex.db.model.enums.ShardType;

public interface ShardedEntity {
    Long getId();
    Long getOrderId();
    Long getShardValue();
    Cluster getCluster();
    Shard getShard();
    ShardType getShardType();
    Boolean isStored();
    void setId(Long id);
    void setStorageAttributes(StorageAttributes storageAttributes);
}
