package com.antalex.domain.persistence.criteria;

import com.antalex.domain.persistence.domain.TestBDomain;
import com.antalex.domain.persistence.entity.shard.TestAShardEntity;
import com.antalex.domain.persistence.entity.shard.TestBShardEntity;
import com.antalex.domain.persistence.entity.shard.TestCShardEntity;
import lombok.Data;

@Data

@Criteria(
        from = TestBShardEntity.class,
        alias = "b",
        joins = {
                @LeftJoin(from = TestAShardEntity.class, alias = "a", on = "a = b.a"),
                @Join(from = TestCShardEntity.class, alias = "c", on = "c.b = b")
        },
        where = "a.value like 'Shard%'",
        cache = CacheType.LAZY
)

public class TestCriteria {
    @CriteriaAttribute("upper(b.newValue)")
    private String newValueB;
    @CriteriaAttribute("a.newValue")
    private String newValueA;
    @CriteriaAttribute("b")
    private TestBDomain bDomain;
}
