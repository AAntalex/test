package com.antalex.domain.persistence.entity.shard;


import com.antalex.db.annotation.ParentShard;
import com.antalex.db.annotation.ShardEntity;
import com.antalex.db.entity.abstraction.BaseShardEntity;
import lombok.Data;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Table(name = "TEST_B")
@Data
//@ShardEntity
public class TestBShardEntity extends BaseShardEntity {
    private String value;
    @ParentShard
    @JoinColumn(name = "C_A_REF")
    private TestAShardEntity a;
    private String newValue;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "C_B_REF")
    private List<TestCShardEntity> cList = new ArrayList<>();
}
