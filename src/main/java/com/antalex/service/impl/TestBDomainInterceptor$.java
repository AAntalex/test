package com.antalex.service.impl;

import com.antalex.domain.persistence.domain.TestBDomain;
import com.antalex.domain.persistence.entity.shard.TestBShardEntity;

import java.util.Date;

public class TestBDomainInterceptor$ extends TestBDomain {
    TestBDomainInterceptor$(TestBShardEntity entity) {
        this.entity = entity;
    }

    @Override
    public void readEntity() {
        TestBShardEntity entity = (TestBShardEntity) this.entity;
        this.setValue(entity.getValue(), false);

        this.isLazy = false;
    }

    @Override
    public String getValue() {
        if (isLazy) {
            readEntity();
        }
        return super.getValue();
    }

    @Override
    public String getNewValue() {
        if (isLazy) {
            readEntity();
        }
        return super.getNewValue();
    }

    @Override
    public Date getDateDoc() {
        if (isLazy) {
            readEntity();
        }
        return super.getDateDoc();
    }

    @Override
    public void setValue(String value) {
        setValue(value, true);
    }
    public void setValue(String value, boolean change) {
        if (isLazy) {
            readEntity();
        }
        if (change) {
            this.setChanges(3);
        }
        super.setValue(value);
    }

    @Override
    public void setNewValue(String newValue) {
        if (isLazy) {
            readEntity();
        }
        super.setNewValue(newValue);
    }
}
