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
import java.util.Date;

@Table(
       indexes = {
               @Index(columnList = "date,num,sum"),
               @Index(columnList = "dateProv")
        })
@Data
@Accessors(chain = true, fluent = true)
@ShardEntity(type = ShardType.MULTI_SHARDABLE)
public class MainDocumEntity extends BaseShardEntity {
    private Integer num;
    private BigDecimal sum;
    private Date date;
    private OffsetDateTime dateProv;
    @ParentShard
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private AccountEntity accDt;
    @ParentShard
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private AccountEntity accCt;
}