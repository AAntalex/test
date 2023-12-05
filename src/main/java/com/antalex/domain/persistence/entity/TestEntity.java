package com.antalex.domain.persistence.entity;


import com.antalex.db.annotation.ShardEntity;
import com.antalex.db.entity.abstraction.BaseShardedEntity;

import javax.persistence.*;

@Table(name = "TEST")
@ShardEntity
public class TestEntity extends BaseShardedEntity {
    @Column(name = "C_CODE")
    private String code;
    @Column(name = "C_VALUE")
    private String value;
}
