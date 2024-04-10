package com.antalex.db.domain.abstraction;

import com.antalex.db.annotation.DomainEntity;
import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.exception.ShardDataBaseException;

public abstract class BaseDomain implements Domain {
    protected ShardInstance entity;

    public BaseDomain () {
        if (this.getClass().isAnnotationPresent(DomainEntity.class)) {
            throw new ShardDataBaseException(
                    String.format(
                            "Запрещено использовать конструктор класса %s напрямую. " +
                                    "Следует использовать DomainEntityManager.newDomain(Class<?>)",
                            this.getClass().getName())
            );
        }
    }

    @Override
    public <T extends ShardInstance> void setEntity(T entity) {
        this.entity = entity;
    }
}
