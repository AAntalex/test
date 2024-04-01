package com.antalex.service.impl;

import com.antalex.db.service.ShardEntityManager;
import com.antalex.domain.persistence.entity.shard.TestAShardEntity;
import com.antalex.domain.persistence.entity.shard.TestBShardEntity;
import com.antalex.domain.persistence.entity.shard.TestCShardEntity;
import com.antalex.profiler.service.ProfilerService;
import com.antalex.service.TestShardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TestShardServiceImpl implements TestShardService {
    @Autowired
    private ShardEntityManager entityManager;
    @Autowired
    private TestBShardEntityRepository testBShardEntityRepository;
    @Autowired
    private ProfilerService profiler;


    @Override
    public List<TestBShardEntity> generate(int cnt, int cntArray, String prefix) {
        List<TestBShardEntity> bList = new ArrayList<>();

        List<TestAShardEntity> aList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TestAShardEntity a = entityManager.newEntity(TestAShardEntity.class);
            a.setValue(prefix + "A" + i);
            a.setNewValue(prefix + "newA" + i);
            aList.add(a);
        }

        for (int i = 0; i < cnt; i++) {
            TestBShardEntity b = entityManager.newEntity(TestBShardEntity.class);

//            TestBShardEntity b = testBShardEntityRepository.factory();



            b.setA(aList.get(i % 10));


            b.setValue(prefix + "B" + i);
            b.setNewValue(prefix + "newB" + i);

            List<TestCShardEntity> cEntities = new ArrayList<>();
            for (int j = 0; j < cntArray; j++) {
                TestCShardEntity c = entityManager.newEntity(TestCShardEntity.class);
                c.setValue(prefix + "C" + (i * cntArray + j));
                c.setNewValue(prefix + "newC" + (i * cntArray + j));
                cEntities.add(c);
            }
            b.getCList().addAll(cEntities);
            bList.add(b);
        }
        return bList;

    }

    @Override
    public void save(List<TestBShardEntity> testBEntities) {
        entityManager.saveAll(testBEntities);
    }

    @Override
    public void update(List<TestBShardEntity> testBEntities) {
        entityManager.updateAll(testBEntities);
    }

    @Override
    public void saveLocal(List<TestBShardEntity> testBEntities) {
        testBShardEntityRepository.saveAll(testBEntities);
    }

    @Override
    public void saveTransactional(List<TestBShardEntity> testBEntities) {
    }
}
