package com.antalex.db.model;

import com.antalex.db.model.enums.ShardType;
import lombok.Data;

@Data
public class StorageAttributes {
    private Cluster cluster;
    private Shard shard;
    private Long shardValue;
    private Boolean stored;
    private ShardType shardType;
}
