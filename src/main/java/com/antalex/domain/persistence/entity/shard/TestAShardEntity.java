package com.antalex.domain.persistence.entity.shard;


import com.antalex.db.annotation.ShardEntity;
import com.antalex.db.entity.abstraction.BaseShardEntity;
import javax.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Table(name = "TEST_A")
@Data
@ShardEntity(cluster = "RAW")
public class TestAShardEntity extends BaseShardEntity {
    private String value;
    private String newValue;
    private LocalDateTime executeTime;
}
