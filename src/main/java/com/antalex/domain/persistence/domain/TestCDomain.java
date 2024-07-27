package com.antalex.domain.persistence.domain;

import com.antalex.domain.persistence.entity.shard.TestCShardEntity;
import lombok.Data;
import ru.vtb.pmts.db.annotation.Attribute;
import ru.vtb.pmts.db.annotation.DomainEntity;
import ru.vtb.pmts.db.domain.abstraction.BaseDomain;

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
