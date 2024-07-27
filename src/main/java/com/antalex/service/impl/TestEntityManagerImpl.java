package com.antalex.service.impl;

import com.antalex.service.TestEntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.vtb.pmts.db.entity.abstraction.ShardInstance;
import ru.vtb.pmts.db.service.ShardEntityManager;

@Service
public class TestEntityManagerImpl implements TestEntityManager {
    @Autowired
    private ShardEntityManager entityManager;

    @Override
    public <T extends ShardInstance> void generateId(T entity) {
        entityManager.generateId(entity);
    }

    @Override
    public <T extends ShardInstance> void setStorage(T entity, ShardInstance parent) {
        entityManager.setStorage(entity, parent);
    }
}
