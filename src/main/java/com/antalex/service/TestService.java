package com.antalex.service;


import com.antalex.domain.persistence.entity.hiber.TestAEntity;
import com.antalex.domain.persistence.entity.hiber.TestBEntity;
import com.antalex.domain.persistence.entity.hiber.TestCEntity;

import java.util.List;

public interface TestService {
    List<TestBEntity> generate(int cnt, int cntArray, TestAEntity testAEntity, String prefix);
    void saveJPA(List<TestBEntity> testBEntities);
    void saveMyBatis(List<TestBEntity> testBEntities);
    void saveTransactionalJPA(List<TestBEntity> testBEntities);
    void save(List<TestBEntity> entities);
    TestBEntity findBByIdMBatis(Long id);
    List<TestCEntity> findAllCMBatis(Long id);
    List<TestBEntity> findAllB(String value);
    TestBEntity findBByIdStatement(Long id);
    List<TestCEntity> findAllCStatement(Long id);
}
