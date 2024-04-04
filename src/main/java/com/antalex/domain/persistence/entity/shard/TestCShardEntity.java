package com.antalex.domain.persistence.entity.shard;


import com.antalex.db.annotation.ShardEntity;
import com.antalex.db.entity.abstraction.BaseShardEntity;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.Date;

@Table(name = "TEST_C")
@Data
@ShardEntity
public class TestCShardEntity extends BaseShardEntity {
    private String value;
    private String newValue;
    @Column(name = "C_B_REF")
    private Long b;
    private Date executeTime;
}
