package com.antalex.service.impl;

import com.antalex.db.domain.abstraction.Domain;
import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.service.DomainEntityMapper;
import com.antalex.domain.persistence.domain.TestBDomain;
import com.antalex.domain.persistence.entity.shard.TestBShardEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class TestBDomainMapper implements DomainEntityMapper<TestBDomain, TestBShardEntity> {
    private ThreadLocal<Map<Long, Domain>> domains = ThreadLocal.withInitial(HashMap::new);

    @Override
    public TestBDomain newDomain(TestBShardEntity entity) {
        return new TestBDomainInterceptor$(entity);
    }

    @Override
    public TestBDomain map(TestBShardEntity entity) {
        if (!Optional.ofNullable(entity).map(ShardInstance::getId).isPresent()) {
            return null;
        }

        TestBDomain domain = (TestBDomain) domains.get().get(entity.getId());
        if (Objects.isNull(domain)) {
            domain = new TestBDomainInterceptor$(entity);
            domains.get().put(entity.getId(), domain);
        }
        domain.setLazy(true);
        return domain;
    }

    @Override
    public TestBShardEntity map(TestBDomain domain) {
        TestBShardEntity entity = domain.getEntity();


        if (domain.isChanged(1)) {
            domain.setValue(entity.getValue());
        }
        domain.setNewValue(entity.getNewValue());
        domain.setExecuteTime(entity.getExecuteTime());

        return entity;
    }

}
