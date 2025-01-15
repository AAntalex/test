package com.antalex.domain.persistence.entity.shard.app;

import com.antalex.db.annotation.ParentShard;
import com.antalex.db.annotation.ShardEntity;
import com.antalex.db.entity.abstraction.BaseShardEntity;
import com.antalex.db.model.enums.ShardType;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Table(
       indexes = {
               @Index(columnList = "code"),
               @Index(columnList = "dateOpen")
        })
@Data
@Accessors(chain = true, fluent = true)
@ShardEntity(type = ShardType.SHARDABLE)
public class AccountEntity extends BaseShardEntity {
    private String code;
    @ParentShard
    @OneToOne
    @JoinColumn
    private ClientEntity clientEntity;
    private BigDecimal saldo;
    private OffsetDateTime dateOpen;
}
