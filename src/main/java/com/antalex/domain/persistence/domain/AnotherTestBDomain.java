package com.antalex.domain.persistence.domain;

import com.antalex.db.annotation.Attribute;
import com.antalex.db.annotation.DomainEntity;
import com.antalex.db.annotation.Storage;
import com.antalex.db.domain.abstraction.BaseDomain;
import com.antalex.db.model.enums.MappingType;
import com.antalex.domain.persistence.entity.shard.TestBShardEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@DomainEntity(
        value = TestBShardEntity.class,
        storages = {
                @Storage(value = TestBDomain.class, cluster = "RAW", name = "TestBDomain"),
                @Storage(cluster = "RAW", name = "routingSection"),
                @Storage(cluster = "RAW", name = "accountingSection"),
        })
@Data
public class AnotherTestBDomain extends BaseDomain {
    @Attribute
    private String value;
    @Attribute
    private String newValue;
    @Attribute
    private Date executeTime;
    @Attribute(mappingType = MappingType.STORAGE)
    private String note;
    @Attribute(name = "a")
    private TestADomain TestA;

    @Attribute(name = "amount", storage = "accountingSection", mappingType = MappingType.STORAGE)
    private BigDecimal sum;
    @Attribute(storage = "routingSection", mappingType = MappingType.STORAGE)
    private Routing routing;

    @Attribute(name = "TestBDomain", mappingType = MappingType.STORAGE)
    private LocalDateTime dateProc;

}
