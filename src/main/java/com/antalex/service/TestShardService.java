package com.antalex.service;


import com.antalex.domain.persistence.entity.shard.TestAShardEntity;
import com.antalex.domain.persistence.entity.shard.TestBShardEntity;

import java.util.List;

public interface TestShardService {
    List<TestBShardEntity> generate(int cnt, int cntArray, TestAShardEntity testAEntity);
    void save(List<TestBShardEntity> testBEntities);
    void saveTransactional(List<TestBShardEntity> testBEntities);
}
