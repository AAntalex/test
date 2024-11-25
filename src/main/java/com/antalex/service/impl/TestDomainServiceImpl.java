package com.antalex.service.impl;

import com.antalex.db.service.DomainEntityManager;
import com.antalex.domain.persistence.domain.Routing;
import com.antalex.domain.persistence.domain.TestADomain;
import com.antalex.domain.persistence.domain.TestBDomain;
import com.antalex.domain.persistence.domain.TestCDomain;
import com.antalex.service.TestDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class TestDomainServiceImpl implements TestDomainService {
    @Autowired
    private DomainEntityManager domainEntityManager;

    @Override
    public List<TestBDomain> generate(int cnt, int cntArray, String prefix) {
        List<TestBDomain> bList = new ArrayList<>();

        List<TestADomain> aList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TestADomain a = domainEntityManager.newDomain(TestADomain.class);
            a.setValue(prefix + "A");
            a.setNewValue(prefix + "newA" + i);
            a.setExecuteTime(LocalDateTime.now());
            aList.add(a);
        }

        for (int i = 0; i < cnt; i++) {
            TestBDomain b = domainEntityManager.newDomain(TestBDomain.class);

            b.setTestA(aList.get(i % 10));

            b.setValue(prefix + "B");
            b.setNewValue(prefix + "newB" + i);
            b.setExecuteTime(OffsetDateTime.now());


            b.setDateDoc(new Date());
            b.setNote(prefix + "B_NOTE");
            b.setNumDoc(1234);
            b.setSum(BigDecimal.valueOf(1.23));
            b.setDateProc(LocalDateTime.now());

            Routing routing = new Routing();
            routing.setName("Test");
            routing.setExecuteTime(LocalDateTime.now());
            b.setRouting(routing);

            List<TestCDomain> cEntities = new ArrayList<>();
            for (int j = 0; j < cntArray; j++) {
                TestCDomain c = domainEntityManager.newDomain(TestCDomain.class);
                c.setValue(prefix + "C");
                c.setNewValue(prefix + "newC" + (i * cntArray + j));
                c.setExecuteTime(LocalDateTime.now());
                cEntities.add(c);
            }
            b.getTestList().addAll(cEntities);
            bList.add(b);
        }
        return bList;
    }

    @Override
    public void save(List<TestBDomain> testBEntities) {
        domainEntityManager.saveAll(testBEntities);
    }

    @Override
    public void update(List<TestBDomain> testBEntities) {
        domainEntityManager.updateAll(testBEntities);
    }
}
