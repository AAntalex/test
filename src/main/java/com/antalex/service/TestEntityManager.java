package com.antalex.service;


import ru.vtb.pmts.db.entity.abstraction.ShardInstance;

public interface TestEntityManager {
    <T extends ShardInstance> void generateId(T entity);
    <T extends ShardInstance> void setStorage(T entity, ShardInstance parent);
}
