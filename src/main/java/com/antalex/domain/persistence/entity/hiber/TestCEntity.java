package com.antalex.domain.persistence.entity.hiber;


import com.antalex.db.entity.abstraction.BaseShardEntity;
import lombok.Data;

import jakarta.persistence.*;
import java.util.Date;

@Table(name = "TEST_C", schema = "main_1")
@Data
@Entity
public class TestCEntity extends BaseShardEntity {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_seq")
    @SequenceGenerator(name = "test_seq", sequenceName = "test_seq_id", schema = "main_1", allocationSize = 1000000)
    private Long id;
    @Column(name = "SHARD_MAP")
    private Long shardMap;
    @Column(name = "C_VALUE")
    private String value;
    @Column(name = "C_B_REF")
    private Long b;
    @Column(name = "C_NEW_VALUE")
    private String newValue;
    @Column(name = "C_EXECUTE_TIME")
    private Date executeTime;
}
