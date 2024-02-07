package com.antalex.service.impl;

import com.antalex.domain.persistence.entity.hiber.TestAEntity;
import com.antalex.domain.persistence.entity.hiber.TestBEntity;
import com.antalex.domain.persistence.entity.hiber.TestCEntity;
import com.antalex.domain.persistence.repository.TestBRepository;
import com.antalex.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TestServiceImpl implements TestService{
    @Autowired
    private TestBRepository testBRepository;

    @Override
    public List<TestBEntity> generate(int cnt, int cntArray, TestAEntity testAEntity) {
        List<TestBEntity> bList = new ArrayList<>();;
        for (int i = 0; i < cnt; i++) {
            TestBEntity b = new TestBEntity();
            b.setA(testAEntity);
            b.setShardValue(1L);
            b.setValue("B" + i);
            b.setNewValue("newB" + i);

            List<TestCEntity> cEntities = new ArrayList<>();
            for (int j = 0; j < cntArray; j++) {
                TestCEntity c = new TestCEntity();
                c.setValue("C" + (i * cntArray + j));
                c.setNewValue("newC" + (i * cntArray + j));
                cEntities.add(c);
            }
            b.getCList().addAll(cEntities);
            bList.add(b);
        }
        return bList;
    }


    @Override
    public void saveJPA(List<TestBEntity> testBEntities) {
        testBRepository.saveAll(testBEntities);
    }

    @Transactional
    @Override
    public void saveTransactionalJPA(List<TestBEntity> testBEntities) {
        saveJPA(testBEntities);
    }
}
