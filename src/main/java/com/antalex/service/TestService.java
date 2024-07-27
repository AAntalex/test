package com.antalex.service;


import com.antalex.domain.persistence.entity.hiber.TestAEntity;
import com.antalex.domain.persistence.entity.hiber.TestBEntity;
import com.antalex.domain.persistence.entity.hiber.TestCEntity;
import com.antalex.service.mapper.MBatisEntityMapper;

import java.sql.PreparedStatement;
import java.util.List;

public interface TestService {
    List<TestBEntity> generate(int cnt, int cntArray, TestAEntity testAEntity, String prefix);
    void saveJPA(List<TestBEntity> testBEntities);
    void saveMyBatis(List<TestBEntity> testBEntities);
    void saveMyBatis(TestBEntity entity);
    void saveTransactionalJPA(List<TestBEntity> testBEntities);
    void save(List<TestBEntity> entities);
    void save(TestBEntity entity);
    TestBEntity findBByIdMBatis(Long id);
    List<TestBEntity> findAllByValueLikeMBatis(String value);
    List<TestCEntity> findAllCMBatis(Long id, MBatisEntityMapper MBatisEntityMapper);
    List<TestBEntity> findAllB(String value, MBatisEntityMapper MBatisEntityMapper);
    TestBEntity findBByIdStatement(Long id);
    List<TestBEntity> findAllBByValueLikeStatement(String value);
    List<TestCEntity> findAllCStatement(Long id, PreparedStatement preparedStatement);
}
