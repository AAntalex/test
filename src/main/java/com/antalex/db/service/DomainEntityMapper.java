package com.antalex.db.service;

import com.antalex.db.domain.abstraction.Domain;
import com.antalex.db.entity.abstraction.ShardInstance;

public interface DomainEntityMapper<T extends Domain, M extends ShardInstance> {
    T newDomain();
    T getDomain(Class<T> clazz, M entity);
}
