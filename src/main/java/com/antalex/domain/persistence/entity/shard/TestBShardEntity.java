package com.antalex.domain.persistence.entity.shard;

import lombok.Data;
import lombok.experimental.Accessors;
import ru.vtb.pmts.db.annotation.ParentShard;
import ru.vtb.pmts.db.annotation.ShardEntity;
import ru.vtb.pmts.db.entity.abstraction.BaseShardEntity;
import ru.vtb.pmts.db.model.enums.ShardType;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;

import java.util.List;

@Table(
        name = "TEST_B",
        indexes = {
                @Index(name = "IDX_TEST_B_VALUE", columnList = "value", unique = false)
        })
@Data
@Accessors(chain = true)
@ShardEntity(type = ShardType.MULTI_SHARDABLE)
public class TestBShardEntity extends BaseShardEntity {
    private String value;
    @ParentShard
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "C_A_REF")
    private TestAShardEntity a;
    private String newValue;
    @ParentShard
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "C_B_REF")
    private List<TestCShardEntity> cList = new ArrayList<>();
    private OffsetDateTime executeTime;
}
