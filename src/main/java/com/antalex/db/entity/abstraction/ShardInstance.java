package com.antalex.db.entity.abstraction;

import com.antalex.db.model.StorageContext;

public interface ShardInstance {
    Long getId();
    Long getOrderId();
    StorageContext getSStorageContext();
    void setId(Long id);
    void setStorageContext(StorageContext storageContext);
    boolean isChanged();
}
