package com.antalex.service.impl;

import com.antalex.db.service.ShardEntityManager;
import com.antalex.domain.persistence.entity.shard.TestAShardEntity;
import com.antalex.domain.persistence.entity.shard.TestBShardEntity;
import com.antalex.domain.persistence.entity.shard.TestBShardEntityExt;
import com.antalex.domain.persistence.entity.shard.TestCShardEntity;
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


    @Override
    public List<TestBShardEntity> generate(int cnt, int cntArray, TestAShardEntity testAEntity) {
        List<TestBShardEntity> bList = new ArrayList<>();;
        for (int i = 0; i < cnt; i++) {
//            TestBShardEntity b = new TestBShardEntityExt();

//            TestBShardEntity b = testBShardEntityRepository.factory();

            TestBShardEntity b;
            try {
                b = TestBShardEntityExt.class.newInstance();
            } catch (Exception err) {
                throw new RuntimeException(err);
            }


            b.setA(testAEntity);
            b.setValue("BShard" + i);
            b.setNewValue("newBShard" + i);

            List<TestCShardEntity> cEntities = new ArrayList<>();
            for (int j = 0; j < cntArray; j++) {
                TestCShardEntity c = new TestCShardEntity();
                c.setValue("CShard" + (i * cntArray + j));
                c.setNewValue("newCShard" + (i * cntArray + j));
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
    public void saveLocal(List<TestBShardEntity> testBEntities) {
        testBShardEntityRepository.saveAll(testBEntities);
    }

    @Override
    public void saveTransactional(List<TestBShardEntity> testBEntities) {
    }
}
