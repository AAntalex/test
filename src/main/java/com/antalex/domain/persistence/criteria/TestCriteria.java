package com.antalex.domain.persistence.criteria;

import com.antalex.db.annotation.CachePolicy;
import com.antalex.db.annotation.Criteria;
import com.antalex.db.annotation.CriteriaAttribute;
import com.antalex.db.annotation.Join;
import com.antalex.db.service.impl.managers.TransactionalCacheManager;
import com.antalex.domain.persistence.domain.TestBDomain;
import com.antalex.domain.persistence.entity.shard.TestAShardEntity;
import com.antalex.domain.persistence.entity.shard.TestBShardEntity;
import com.antalex.domain.persistence.entity.shard.TestCShardEntity;
import lombok.Data;

import javax.persistence.FetchType;
import javax.persistence.criteria.JoinType;

@Data

@Criteria(
        from = TestBShardEntity.class,
        alias = "b",
        joins = {
                @Join(from = TestAShardEntity.class, joinType = JoinType.LEFT, alias = "a", on = "${a} = ${b.a}"),
                @Join(from = TestCShardEntity.class, alias = "c", on = "${c.b} = ${b}")
        },
        where = "${a.value} like 'Shard%'",
        cachePolicy = @CachePolicy(
                fetch = FetchType.EAGER,
                implement = TransactionalCacheManager.class,
                key = {"b.value", "b.newValue"},
                retentionTime = 60
        )
)


public class TestCriteria {
    @CriteriaAttribute("upper(${b.newValue})")
    private String newValueB;
    @CriteriaAttribute("a.newValue")
    private String newValueA;
    @CriteriaAttribute("b")
    private TestBDomain bDomain;
}
