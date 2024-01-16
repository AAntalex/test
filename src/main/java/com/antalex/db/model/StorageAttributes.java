package com.antalex.db.model;

import lombok.Data;

@Data
public class StorageAttributes {
    private Cluster cluster;
    private Shard shard;
    private Long shardValue;
    private Boolean stored;
}
