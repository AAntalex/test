package com.antalex.domain.persistence.entity.shard;


import javax.persistence.EntityManager;

public class TestBShardEntityExt extends TestBShardEntity {
    EntityManager entityManager;

    @Override
    public void setNewValue(String newValue) {
        super.setNewValue(newValue);
        entityManager.getTransaction();

    }
}
