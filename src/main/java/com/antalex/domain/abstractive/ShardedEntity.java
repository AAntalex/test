package com.antalex.domain.abstractive;

import java.util.Objects;

public abstract class ShardedEntity implements Sharded {
    protected String id;
    protected Short cluster;
    protected Long shardValue;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
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
}
