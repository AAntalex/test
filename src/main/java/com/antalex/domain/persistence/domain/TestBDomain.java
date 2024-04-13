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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@DomainEntity(
        value = TestBShardEntity.class,
        additionalStorage = {
                @Storage(value = "routingSection", cluster = "RAW"),
                @Storage(value = "accountingSection", cluster = "RAW"),
        })
@Data
public class TestBDomain extends BaseDomain {
    @Attribute
    private String value;
    @Attribute
    private String newValue;
    @Attribute
    private LocalDateTime executeTime;
    @Attribute(name = "a")
    private TestADomain TestA;
    @Attribute(name = "cList")
    private List<TestCDomain> testList = new ArrayList<>();

    @Attribute(mappingType = MappingType.STORAGE)
    private String note;
    @Attribute(mappingType = MappingType.STORAGE)
    private LocalDateTime dateProc;

    @Attribute(name = "amount", storage = "accountingSection", mappingType = MappingType.STORAGE)
    private BigDecimal sum;
    @Attribute(storage = "accountingSection", mappingType = MappingType.STORAGE)
    private Integer numDoc;
    @Attribute(storage = "accountingSection", mappingType = MappingType.STORAGE)
    private Date dateDoc;

    @Attribute(storage = "routingSection", mappingType = MappingType.STORAGE)
    private Routing routing;

}
