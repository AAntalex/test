package com.antalex.domain.persistence.entity.shard;


import lombok.Data;
import ru.vtb.pmts.db.annotation.ShardEntity;
import ru.vtb.pmts.db.entity.abstraction.BaseShardEntity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Table(name = "TEST_A")
@Data
@ShardEntity(cluster = "RAW")
public class TestAShardEntity extends BaseShardEntity {
    private String value;
    private String newValue;
    private LocalDateTime executeTime;
}
