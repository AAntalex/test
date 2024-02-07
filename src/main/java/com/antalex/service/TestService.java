package com.antalex.service;


import com.antalex.domain.persistence.entity.hiber.TestAEntity;
import com.antalex.domain.persistence.entity.hiber.TestBEntity;

import java.util.List;

public interface TestService {
    List<TestBEntity> generate(int cnt, int cntArray, TestAEntity testAEntity);
    void saveJPA(List<TestBEntity> testBEntities);
    void saveTransactionalJPA(List<TestBEntity> testBEntities);
}
