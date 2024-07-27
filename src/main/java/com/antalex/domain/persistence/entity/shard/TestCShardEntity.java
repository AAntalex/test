package com.antalex.domain.persistence.entity.shard;


import lombok.Data;
import ru.vtb.pmts.db.annotation.ShardEntity;
import ru.vtb.pmts.db.entity.abstraction.BaseShardEntity;

import javax.persistence.Column;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Table(name = "TEST_C")
@Data
@ShardEntity
public class TestCShardEntity extends BaseShardEntity {
    private String value;
    private String newValue;
    @Column(name = "C_B_REF")
    private Long b;
    private LocalDateTime executeTime;
}
