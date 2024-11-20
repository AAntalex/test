package com.antalex.domain.persistence.criteria;

import com.antalex.domain.persistence.domain.TestBDomain;
import com.antalex.domain.persistence.entity.shard.TestAShardEntity;
import com.antalex.domain.persistence.entity.shard.TestBShardEntity;
import com.antalex.domain.persistence.entity.shard.TestCShardEntity;
import lombok.Data;

import ru.vtb.pmts.db.annotation.CachePolicy;
import ru.vtb.pmts.db.annotation.Criteria;
import ru.vtb.pmts.db.annotation.CriteriaAttribute;
import ru.vtb.pmts.db.annotation.Join;
import ru.vtb.pmts.db.service.impl.managers.TransactionalCacheManager;

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
