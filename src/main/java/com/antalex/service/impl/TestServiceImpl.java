package com.antalex.service.impl;

import com.antalex.domain.persistence.entity.hiber.TestAEntity;
import com.antalex.domain.persistence.entity.hiber.TestBEntity;
import com.antalex.domain.persistence.repository.TestARepository;
import com.antalex.domain.persistence.repository.TestBRepository;
import com.antalex.domain.persistence.repository.TestCRepository;
import com.antalex.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestServiceImpl implements TestService{
    @Autowired
    private TestARepository testARepository;
    @Autowired
    private TestBRepository testBRepository;
    @Autowired
    private TestCRepository testCRepository;

    @Override
    public void saveJPA() {
        TestAEntity a = new TestAEntity();
        a.setValue("A1");
        a.setValue("newA1");

        System.out.println("AAA BEFORE SAVE A");
        testARepository.save(a);
        System.out.println("AAA AFTER SAVE A");

        TestBEntity b = new TestBEntity();
        b.setA(a);
        b.setShardValue(1L);
        b.setValue("B1");
        b.setNewValue("newB1");

        System.out.println("AAA BEFORE SAVE B");
        testBRepository.save(b);
        System.out.println("AAA AFTER SAVE B");

        b.setNewValue("newB2");


        System.out.println("AAA BEFORE SAVE B");
        testBRepository.save(b);
        System.out.println("AAA AFTER SAVE B");

        System.out.println("AAA BEFORE SAVE B");
        testBRepository.save(b);
        System.out.println("AAA AFTER SAVE B");
    }

    @Transactional
    @Override
    public void saveTransactionalJPA() {
        saveJPA();
    }
}
