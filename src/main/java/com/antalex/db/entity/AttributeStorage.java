package com.antalex.db.entity;

import com.antalex.db.entity.abstraction.BaseShardEntity;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.enums.DataFormat;
import com.antalex.db.model.enums.ShardType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;

@Data
public class AttributeStorage extends BaseShardEntity {
    private Long entityId;
    private String storageName;
    private String data;
    private DataFormat dataFormat;
    private Cluster cluster;
    private ShardType shardType;
}
