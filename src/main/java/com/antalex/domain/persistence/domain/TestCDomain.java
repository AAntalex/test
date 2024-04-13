package com.antalex.domain.persistence.domain;

import com.antalex.db.annotation.Attribute;
import com.antalex.db.annotation.DomainEntity;
import com.antalex.db.domain.abstraction.BaseDomain;
import com.antalex.domain.persistence.entity.shard.TestCShardEntity;
import lombok.Data;

@DomainEntity(TestCShardEntity.class)
@Data
public class TestCDomain extends BaseDomain {
    @Attribute
    private String value;
}
