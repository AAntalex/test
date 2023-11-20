package com.antalex.domain.persistence.entity;

import com.antalex.annotation.ShardEntity;
import lombok.Data;

import javax.persistence.*;

@Table(name = "TEST")
@ShardEntity
public class TestEntity {
    @Column(name = "C_CODE")
    private String code;
    @Column(name = "C_VALUE")
    private String value;
}
