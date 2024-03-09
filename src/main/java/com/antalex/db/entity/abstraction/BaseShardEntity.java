package com.antalex.db.entity.abstraction;

import com.antalex.db.annotation.ShardEntity;
import com.antalex.db.model.StorageContext;
import com.antalex.db.service.impl.SharedEntityTransaction;
import com.antalex.db.utils.ShardUtils;

import javax.persistence.EntityTransaction;
import java.util.Optional;

public abstract class BaseShardEntity implements ShardInstance {
    protected Long id;
    private StorageContext storageContext;
    private Boolean changed;

    public BaseShardEntity () {
        if (this.getClass().isAnnotationPresent(ShardEntity.class)) {
            throw new RuntimeException(
                    String.format(
                            "Запрещено использовать конструктор класса %s напрямую. " +
                                    "Следует использовать ShardEntityManager.newEntity(Class<?>)",
                            this.getClass().getName())
            );
        }
    }

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
    public StorageContext getStorageContext() {
        return storageContext;
    }

    @Override
    public void setStorageContext(StorageContext storageContext) {
        this.storageContext = storageContext;
    }

    @Override
    public boolean isChanged() {
        return Optional.ofNullable(this.storageContext)
                .map(StorageContext::isChanged)
                .orElse(false);
    }


    @Override
    public Boolean isStored() {
        return Optional.ofNullable(this.storageContext)
                .map(StorageContext::isStored)
                .orElse(false);
    }

    @Override
    public void setChanged() {
        if (this.storageContext != null) {
            this.storageContext.setChanged();
        }
    }

    @Override
    public boolean hasNewShards() {
        return Optional.ofNullable(this.storageContext)
                .map(StorageContext::hasNewShards)
                .orElse(false);
    }

    @Override
    public boolean setTransactionalContext(EntityTransaction transaction) {
        return this.storageContext != null &&
                this.storageContext.setTransactionalContext((SharedEntityTransaction) transaction);
    }
}
