package com.antalex.service;


import com.antalex.domain.persistence.entity.shard.TestBShardEntity;
import com.antalex.domain.persistence.entity.shard.TestOtherShardEntity;

import java.util.List;

public interface TestShardService {
    List<TestBShardEntity> generate(int cnt, int cntArray, String prefix);
    void save(List<TestBShardEntity> testBEntities);
    List<TestOtherShardEntity> generateOther(int cnt);
    void saveOther(List<TestOtherShardEntity> entities);
    void update(List<TestBShardEntity> testBEntities);
    void saveLocal(List<TestBShardEntity> testBEntities);
    void saveTransactional(List<TestBShardEntity> testBEntities);
}
