package com.antalex.domain.persistence.entity.shard.app;

import com.antalex.db.annotation.ShardEntity;
import com.antalex.db.entity.abstraction.BaseShardEntity;
import com.antalex.db.model.enums.ShardType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

@EqualsAndHashCode(callSuper = true)
@Table
@Data
@ShardEntity(type = ShardType.REPLICABLE)
public class ClientCategory extends BaseShardEntity {
    private String code;
}
