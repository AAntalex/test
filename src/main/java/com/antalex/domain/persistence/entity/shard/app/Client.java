package com.antalex.domain.persistence.entity.shard.app;

import com.antalex.db.annotation.ShardEntity;
import com.antalex.db.entity.abstraction.BaseShardEntity;
import com.antalex.db.model.enums.ShardType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.persistence.*;

@EqualsAndHashCode(callSuper = true)
@Table(
       indexes = {
                @Index(columnList = "name")
        })
@Data
@Accessors(chain = true, fluent = true)
@ShardEntity(type = ShardType.SHARDABLE)
public class Client extends BaseShardEntity {
    private String name;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private ClientCategory category;
}
