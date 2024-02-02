package com.antalex.db.entity.abstraction;

import com.antalex.db.model.StorageAttributes;

public interface ShardInstance {
    Long getId();
    Long getOrderId();
    StorageAttributes getStorageAttributes();
    void setId(Long id);
    void setStorageAttributes(StorageAttributes storageAttributes);
}
