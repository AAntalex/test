package com.antalex.domain.persistence.entity.hiber;


import lombok.Data;
import ru.vtb.pmts.db.entity.abstraction.BaseShardEntity;

import jakarta.persistence.*;
import java.util.Date;

@Table(name = "TEST_C", schema = "pmts_integr")
@Data
@Entity
public class TestCEntity extends BaseShardEntity {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_seq")
    @SequenceGenerator(name = "test_seq", sequenceName = "test_seq_id", schema = "pmts_integr", allocationSize = 1000000)
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
