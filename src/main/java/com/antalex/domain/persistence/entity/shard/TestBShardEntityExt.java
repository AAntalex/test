package com.antalex.domain.persistence.entity.shard;


public class TestBShardEntityExt extends TestBShardEntity {
    public TestBShardEntityExt() {
        super.setNewValue("AAA");
    }

    @Override
    public void setNewValue(String newValue) {
        super.setNewValue(newValue);
    }
}
