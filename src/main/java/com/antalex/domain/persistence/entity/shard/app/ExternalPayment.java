package com.antalex.domain.persistence.entity.shard.app;

import com.antalex.db.annotation.ShardEntity;
import com.antalex.db.entity.abstraction.BaseShardEntity;
import com.antalex.db.model.enums.ShardType;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.Date;

@Table(
       indexes = {
               @Index(columnList = "date"),
               @Index(columnList = "doc")
        })
@Data
@Accessors(chain = true, fluent = true)
@ShardEntity(type = ShardType.SHARDABLE)
public class ExternalPayment extends BaseShardEntity {
    private String receiver;
    private Date date;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private MainDocum doc;
}
