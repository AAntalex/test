package com.antalex.domain.persistence.entity.hiber;


import com.antalex.db.entity.abstraction.BaseShardEntity;
import lombok.Data;

import javax.persistence.*;

@Table(name = "TEST_C")
@Data
@Entity
public class TestCEntity extends BaseShardEntity {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_id")
    @SequenceGenerator(name = "seq_id", sequenceName = "SEQ_ID")
    private Long id;
    @Column(name = "SHARD_VALUE")
    private Long shardValue;
    @Column(name = "C_VALUE")
    private String value;
    @Column(name = "C_B_REF")
    private Long b;
    @Column(name = "C_NEW_VALUE")
    private String newValue;
}
