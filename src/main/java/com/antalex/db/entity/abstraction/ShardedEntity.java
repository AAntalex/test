package com.antalex.db.entity.abstraction;

public interface ShardedEntity {
    Long getId();
    Long getShardValue();
    Short getCluster();
}
