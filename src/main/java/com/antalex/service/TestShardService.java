package com.antalex.service;


import com.antalex.domain.persistence.entity.shard.TestAShardEntity;
import com.antalex.domain.persistence.entity.shard.TestBShardEntity;

import java.util.List;

public interface TestShardService {
    List<TestBShardEntity> generate(int cnt, int cntArray, String prefix);
    void save(List<TestBShardEntity> testBEntities);
    void update(List<TestBShardEntity> testBEntities);
    void saveLocal(List<TestBShardEntity> testBEntities);
    void saveTransactional(List<TestBShardEntity> testBEntities);
}
