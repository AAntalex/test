package com.antalex.domain.persistence.domain;

import com.antalex.db.annotation.Attribute;
import com.antalex.db.annotation.DomainEntity;
import com.antalex.db.domain.abstraction.BaseDomain;
import com.antalex.domain.persistence.entity.shard.TestCShardEntity;
import lombok.Data;

import java.time.LocalDateTime;

@DomainEntity(TestCShardEntity.class)
@Data
public class TestCDomain extends BaseDomain {
    @Attribute
    private String value;
    @Attribute
    private String newValue;
    @Attribute
    private LocalDateTime executeTime;
}
