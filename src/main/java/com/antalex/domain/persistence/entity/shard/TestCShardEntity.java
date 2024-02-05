package com.antalex.domain.persistence.entity.shard;


import com.antalex.db.annotation.ParentShard;
import com.antalex.db.annotation.ShardEntity;
import com.antalex.db.entity.abstraction.BaseShardEntity;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Table;

@Table(name = "TEST_C")
@Data
@ShardEntity
public class TestCShardEntity extends BaseShardEntity {
    private String value;
    private String newValue;
    @ParentShard
    @Column(name = "C_B_REF")
    private TestBShardEntity b;
}
