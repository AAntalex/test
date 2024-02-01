package com.antalex.db.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StorageAttributes {
    private Cluster cluster;
    private Shard shard;
    private Long shardValue;
    private Boolean stored;
}
