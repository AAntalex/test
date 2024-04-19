package com.antalex.db.service;

import com.antalex.db.domain.abstraction.Domain;
import com.antalex.db.entity.abstraction.ShardInstance;

public interface DomainEntityMapper<T extends Domain, M extends ShardInstance> {
    T newDomain(M entity);
    T map(M entity);
    M map(T domain);
    void setDomainManager(DomainEntityManager domainManager);
}
