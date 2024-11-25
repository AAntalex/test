package com.antalex.domain.persistence.entity.hiber;


import com.antalex.db.entity.abstraction.BaseShardEntity;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Table(name = "TEST_A", schema = "segment_integr")
@Data
@Entity
public class TestAEntity extends BaseShardEntity {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_seq")
    @SequenceGenerator(name = "test_seq", sequenceName = "test_seq_id", schema = "segment_integr", allocationSize = 1000000)
    private Long id;
    @Column(name = "SHARD_MAP")
    private Long shardMap;
    @Column(name = "C_VALUE")
    private String value;
    @Column(name = "C_NEW_VALUE")
    private String newValue;
    @Column(name = "C_EXECUTE_TIME")
    private LocalDateTime executeTime;
}
