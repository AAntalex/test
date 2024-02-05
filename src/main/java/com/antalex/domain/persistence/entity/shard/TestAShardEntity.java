package com.antalex.domain.persistence.entity.shard;


import com.antalex.db.annotation.ShardEntity;
import com.antalex.db.entity.abstraction.BaseShardEntity;
import lombok.Data;

import javax.persistence.*;

@Table(name = "TEST_A")
@Data
@ShardEntity
public class TestAShardEntity extends BaseShardEntity {
    private String value;
    private String newValue;
}
