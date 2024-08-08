package com.antalex.domain.persistence.domain;

import com.antalex.domain.persistence.entity.shard.TestAShardEntity;
import lombok.Data;
import ru.vtb.pmts.db.annotation.Attribute;
import ru.vtb.pmts.db.annotation.DomainEntity;
import ru.vtb.pmts.db.domain.abstraction.BaseDomain;

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
