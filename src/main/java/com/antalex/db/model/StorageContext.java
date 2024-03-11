package com.antalex.db.model;

import com.antalex.db.service.impl.SharedEntityTransaction;
import lombok.Builder;
import lombok.Data;

import java.util.Optional;

@Builder
public class StorageContext {
    private Cluster cluster;
    private Shard shard;
    private Long shardValue;
    private Long originalShardValue;
    private Boolean stored;
    private Boolean changed;
    private boolean temporary;
    private TransactionalContext transactionalContext;

    public boolean setTransactionalContext(SharedEntityTransaction transaction) {
        if (transaction == null) {
            return false;
        }
        if (this.transactionalContext == null) {
            this.transactionalContext = new TransactionalContext();
            this.transactionalContext.setStored(this.stored);
            this.transactionalContext.setChanged(this.changed);
            this.transactionalContext.setOriginalShardValue(this.originalShardValue);
            this.transactionalContext.setTransaction(transaction);
            this.transactionalContext.setPersist(false);
            return true;
        }
        if (!this.transactionalContext.getPersist() && this.transactionalContext.getTransaction() == transaction) {
            return false;
        }
        Optional.ofNullable(this.transactionalContext.getTransaction())
                .filter(SharedEntityTransaction::isCompleted)
                .ifPresent(it -> {
                    if (it.hasError()) {
                        this.transactionalContext.setChanged(this.changed);
                        this.transactionalContext.setStored(this.stored);
                        this.transactionalContext.setOriginalShardValue(this.originalShardValue);
                    } else {
                        this.changed = this.transactionalContext.getChanged();
                        this.stored = this.transactionalContext.getStored();
                        this.originalShardValue = this.transactionalContext.getOriginalShardValue();
                    }
                });
        this.transactionalContext.setPersist(false);
        this.transactionalContext.setTransaction(transaction);
        return true;
    }

    public void persist() {
        if (this.transactionalContext != null) {
            this.transactionalContext.setChanged(false);
            this.transactionalContext.setStored(true);
            this.transactionalContext.setOriginalShardValue(this.shardValue);
            this.transactionalContext.setPersist(true);
        }
    }

    public void setChanged() {
        this.changed = true;
        if (this.transactionalContext != null) {
            this.transactionalContext.setChanged(true);
        }
    }

    public Boolean isChanged() {
        return Optional.ofNullable(this.transactionalContext)
                .filter(it -> !it.transaction.hasError())
                .map(TransactionalContext::getChanged)
                .orElse(this.changed);
    }

    public Boolean isStored() {
        return Optional.ofNullable(this.transactionalContext)
                .filter(it -> !it.transaction.hasError())
                .map(TransactionalContext::getStored)
                .orElse(this.stored);
    }

    public Long getOriginalShardValue() {
        return Optional.ofNullable(this.transactionalContext)
                .filter(it -> !it.transaction.hasError())
                .map(TransactionalContext::getOriginalShardValue)
                .orElse(this.originalShardValue);
    }

    public boolean hasNewShards() {
        return Optional.ofNullable(getOriginalShardValue())
                .map(it -> !it.equals(this.shardValue))
                .orElse(false);
    }

    public Shard getShard() {
        return shard;
    }

    public void setShard(Shard shard) {
        this.shard = shard;
    }

    public Long getShardValue() {
        return shardValue;
    }

    public void setShardValue(Long shardValue) {
        this.shardValue = shardValue;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public boolean isTemporary() {
        return temporary;
    }

    @Data
    private class TransactionalContext {
        private SharedEntityTransaction transaction;
        private Boolean changed;
        private Long originalShardValue;
        private Boolean stored;
        private Boolean persist;
    }
}
