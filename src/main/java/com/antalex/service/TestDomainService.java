package com.antalex.service;


import com.antalex.domain.persistence.domain.TestBDomain;

import java.util.List;

public interface TestDomainService {
    List<TestBDomain> generate(int cnt, int cntArray, String prefix);
    void save(List<TestBDomain> testBEntities);
    void update(List<TestBDomain> testBEntities);
    List<TestBDomain> findAllB();
}
