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
    private Boolean temporary;
    private Boolean changed;
    private TransactionalContext transactionalContext;

    public void setTransactionalContext(SharedEntityTransaction transaction) {
        if (this.transactionalContext == null) {
            this.transactionalContext =
                    TransactionalContext
                            .builder()
                            .changed(this.changed)
                            .stored(this.stored)
                            .originalShardValue(this.originalShardValue)
                            .build();
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
        this.transactionalContext.setTransaction(transaction);
    }

    public void persist() {
        if (this.transactionalContext != null) {
            this.transactionalContext.setChanged(false);
            this.transactionalContext.setStored(true);
            this.transactionalContext.setOriginalShardValue(this.shardValue);
        }
    }

    public void setChanged() {
        this.changed = true;
        if (this.transactionalContext != null) {
            this.transactionalContext.setChanged(true);
        }
    }

    @Data
    @Builder
    private class TransactionalContext {
        private SharedEntityTransaction transaction;
        private Boolean changed;
        private Long originalShardValue;
        private Boolean stored;
    }
}
