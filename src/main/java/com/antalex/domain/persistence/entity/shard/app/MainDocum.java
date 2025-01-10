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
               @Index(columnList = "num,sum"),
               @Index(columnList = "dateProv")
        })
@Data
@Accessors(chain = true)
@ShardEntity(type = ShardType.MULTI_SHARDABLE)
public class MainDocum extends BaseShardEntity {
    private Integer num;
    private BigDecimal sum;
    private OffsetDateTime dateProv;
    @ParentShard
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private Account accDt;
    @ParentShard
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private Account accCt;
}
