package com.antalex.domain.persistence.entity.shard.app;

import com.antalex.db.annotation.ShardEntity;
import com.antalex.db.entity.abstraction.BaseShardEntity;
import com.antalex.db.model.enums.ShardType;
import lombok.Data;

import javax.persistence.*;

@Table
@Data
@ShardEntity(type = ShardType.REPLICABLE)
public class ClientCategoryEntity extends BaseShardEntity {
    private String code;
}
