package com.antalex.domain.persistence.entity;


import com.antalex.db.annotation.ShardEntity;
import com.antalex.db.entity.abstraction.BaseShardEntity;

import javax.persistence.*;

@Table(name = "T_TEST")
@ShardEntity
public class TestEntity extends BaseShardEntity {
    @Column(name = "C_CODE")
    private String code;
    @Column(name = "C_VALUE")
    private String value;
}
