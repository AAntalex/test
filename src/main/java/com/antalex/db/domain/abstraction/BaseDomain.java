package com.antalex.db.domain.abstraction;

import com.antalex.db.annotation.DomainEntity;
import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.exception.ShardDataBaseException;
import com.antalex.db.utils.Utils;

import java.util.Objects;

public abstract class BaseDomain implements Domain {
    protected ShardInstance entity;
    protected boolean isLazy;
    private Long changes;

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
    @SuppressWarnings("unchecked")
    public <T extends ShardInstance> T getEntity() {
        return (T) entity;
    }

    public void readEntity() {

    }

    public boolean isLazy() {
        return isLazy;
    }

    public void setLazy(boolean lazy) {
        isLazy = lazy;
    }

    public void setChanges(int index) {
        this.changes = Utils.addChanges(index, this.changes);
    }

    public Boolean isChanged(int index) {
        return Utils.isChanged(index, this.changes);
    }

    public Boolean isChanged() {
        return Objects.nonNull(this.changes);
    }
}
