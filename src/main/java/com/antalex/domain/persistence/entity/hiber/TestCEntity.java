package com.antalex.domain.persistence.entity.hiber;


import com.antalex.db.entity.abstraction.BaseShardEntity;
import lombok.Data;

import javax.persistence.*;

@Table(name = "TEST_C", schema = "pmts_main")
@Data
@Entity
public class TestCEntity extends BaseShardEntity {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_id")
    @SequenceGenerator(name = "seq_id", sequenceName = "SEQ_ID", schema = "pmts_main")
    private Long id;
    @Column(name = "SHARD_MAP")
    private Long shardMap;
    @Column(name = "C_VALUE")
    private String value;
    @OneToOne
    @JoinColumn(name = "C_B_REF")
    private TestBEntity b;
    @Column(name = "C_NEW_VALUE")
    private String newValue;
}
