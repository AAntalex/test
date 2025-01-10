package com.antalex.domain.persistence.entity.shard.app;

import com.antalex.db.annotation.ShardEntity;
import com.antalex.db.entity.abstraction.BaseShardEntity;
import com.antalex.db.model.enums.ShardType;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;

@Table(
       indexes = {
                @Index(columnList = "value")
        })
@Data
@Accessors(chain = true)
@ShardEntity(type = ShardType.SHARDABLE)
public class Client extends BaseShardEntity {
    private String name;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private ClientCategory category;
}
