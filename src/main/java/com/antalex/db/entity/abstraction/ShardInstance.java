package com.antalex.db.entity.abstraction;

import com.antalex.db.model.StorageContext;

import javax.persistence.EntityTransaction;

public interface ShardInstance {
    Long getId();
    Long getOrderId();
    StorageContext getStorageContext();
    void setId(Long id);
    void setStorageContext(StorageContext storageContext);
    boolean isChanged();
    Boolean isStored();
    void setChanged();
    boolean hasNewShards();
    boolean setTransactionalContext(EntityTransaction transaction);
}
