package com.antalex.db.entity.abstraction;

import com.antalex.db.model.Shard;
import com.antalex.db.model.StorageContext;

import javax.persistence.EntityTransaction;

public interface ShardInstance {
    Long getId();
    Long getOrderId();
    StorageContext getStorageContext();
    void setId(Long id);
    void setStorageContext(StorageContext storageContext);
    boolean isChanged();
    boolean isChanged(int index);
    Long getChanges();
    Boolean isStored();
    void setChanges(int index);
    boolean hasNewShards();
    boolean hasMainShard();
    boolean isOurShard(Shard shard);
    boolean isLazy();
    boolean setTransactionalContext(EntityTransaction transaction);
    void setShardMap(Long shardMap);
}
