package com.antalex.domain.persistence.entity.hiber;


import jakarta.persistence.*;
import lombok.Data;
import ru.vtb.pmts.db.entity.abstraction.BaseShardEntity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Table(name = "TEST_B", schema = "segment_integr")
@Data
@Entity
public class TestBEntity extends BaseShardEntity {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_seq")
    @SequenceGenerator(name = "test_seq", sequenceName = "test_seq_id", schema = "segment_integr", allocationSize = 1000000)
    private Long id;
    @Column(name = "SHARD_MAP")
    private Long shardMap;
    @Column(name = "C_VALUE")
    private String value;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "C_A_REF")
    private TestAEntity a;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "C_B_REF")
    private List<TestCEntity> cList = new ArrayList<>();
    @Column(name = "C_NEW_VALUE")
    private String newValue;
    @Column(name = "C_EXECUTE_TIME")
    private Date executeTime;
}
