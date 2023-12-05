package com.antalex.db.entity.abstraction;

import java.util.Objects;

public abstract class BaseShardedEntity implements ShardedEntity {
    protected Long id;
    protected Short cluster;
    protected Long shardValue;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        if (Objects.nonNull(id) && Objects.isNull(cluster)) {

        }
        this.id = id;
    }

    @Override
    public Short getCluster() {
        if (Objects.nonNull(id) && Objects.isNull(cluster)) {

        }
        return cluster;
    }

    public void setCluster(Short cluster) {
        this.cluster = cluster;
    }

    @Override
    public Long getShardValue() {
        return shardValue;
    }
}
