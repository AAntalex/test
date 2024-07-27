package com.antalex.service.impl;

import com.antalex.domain.persistence.entity.shard.TestAShardEntity;
import ru.vtb.pmts.db.service.ShardEntityManager;

import java.time.LocalDateTime;

public class TestAShardEntityInterceptor extends TestAShardEntity {
    private ShardEntityManager entityManager;
    public void setEntityManager(ShardEntityManager entityManager) {
        this.entityManager = entityManager;
    }


    public void init() {
    }

    @Override
    public String getValue() {
        if (this.isLazy()) {
            entityManager.find(this);
        }
        return super.getValue();
    }
    @Override
    public String getNewValue() {
        if (this.isLazy()) {
            entityManager.find(this);
        }
        return super.getNewValue();
    }
    @Override
    public LocalDateTime getExecuteTime() {
        if (this.isLazy()) {
            entityManager.find(this);
        }
        return super.getExecuteTime();
    }

    public void setValue(String value, boolean change) {
        if (change) {
            if (this.isLazy()) {
                entityManager.find(this);
            }
            this.setChanges(1);
        }
        super.setValue(value);
    }
    @Override
    public void setValue(String value) {
        setValue(value, true);
    }
    public TestAShardEntityInterceptor value(String value) {
        setValue(value);
        return this;
    }

    public void setNewValue(String value, boolean change) {
        if (change) {
            if (this.isLazy()) {
                entityManager.find(this);
            }
            this.setChanges(2);
        }
        super.setNewValue(value);
    }
    @Override
    public void setNewValue(String value) {
        setNewValue(value, true);
    }
    public void setExecuteTime(LocalDateTime value, boolean change) {
        if (change) {
            if (this.isLazy()) {
                entityManager.find(this);
            }
            this.setChanges(3);
        }
        super.setExecuteTime(value);
    }
    @Override
    public void setExecuteTime(LocalDateTime value) {
        setExecuteTime(value, true);
    }
}
