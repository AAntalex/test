package com.antalex.domain.persistence.entity.shard;


public class TestBShardEntityExt extends TestBShardEntity {
    @Override
    public TestBShardEntity setNewValue(String newValue) {
        super.setNewValue(newValue);
        return this;
    }
}
