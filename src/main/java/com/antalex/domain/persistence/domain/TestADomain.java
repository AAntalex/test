package com.antalex.domain.persistence.domain;

import com.antalex.db.annotation.Attribute;
import com.antalex.db.annotation.DomainEntity;
import com.antalex.db.domain.abstraction.BaseDomain;
import com.antalex.domain.persistence.entity.shard.TestAShardEntity;
import lombok.Data;

import java.time.LocalDateTime;

@DomainEntity(TestAShardEntity.class)
@Data
public class TestADomain extends BaseDomain {
    @Attribute
    private String value;
    @Attribute
    private LocalDateTime executeTime;

    private String newValue;
}
