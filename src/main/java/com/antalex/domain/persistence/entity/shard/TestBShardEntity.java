package com.antalex.domain.persistence.entity.shard;


import com.antalex.db.annotation.ParentShard;
import com.antalex.db.annotation.ShardEntity;
import com.antalex.db.entity.abstraction.BaseShardEntity;
import com.antalex.db.model.enums.ShardType;
import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Table(
        name = "TEST_B"/*,
        indexes = {
                @Index(name = "IDX_TEST_B_VALUE", columnList = "newValue,value", unique = true)
        }*/)
@Data
@ShardEntity(type = ShardType.MULTI_SHARDABLE)
public class TestBShardEntity extends BaseShardEntity {
    private String value;
    @ParentShard
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "C_A_REF")
    private TestAShardEntity a;
    private String newValue;
    @ParentShard
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "C_B_REF")
    private List<TestCShardEntity> cList = new ArrayList<>();
    private Date executeTime;
}
