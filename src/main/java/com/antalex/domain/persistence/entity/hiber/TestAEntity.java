package com.antalex.domain.persistence.entity.hiber;


import com.antalex.db.entity.abstraction.BaseShardEntity;
import lombok.Data;

import javax.persistence.*;

@Table(name = "TEST_A", schema = "pmts_public_1")
@Data
@Entity
public class TestAEntity extends BaseShardEntity {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_id")
    @SequenceGenerator(name = "seq_id", sequenceName = "SEQ_ID", schema = "pmts_public_1")
    private Long id;
    @Column(name = "SHARD_MAP")
    private Long shardMap;

    @Column(name = "C_VALUE")
    private String value;
    @Column(name = "C_NEW_VALUE")
    private String newValue;
}
