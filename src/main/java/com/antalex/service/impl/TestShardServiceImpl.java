package com.antalex.service.impl;

import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.ShardEntityManager;
import com.antalex.domain.persistence.entity.shard.*;
import com.antalex.profiler.service.ProfilerService;
import com.antalex.service.TestShardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class TestShardServiceImpl implements TestShardService {
    @Autowired
    private ShardEntityManager entityManager;
    @Autowired
    private ShardDataBaseManager dataBaseManager;
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
            a.setValue(prefix + "A");
            a.setNewValue(prefix + "newA" + i);
            a.setExecuteTime(LocalDateTime.now());
            aList.add(a);
        }

        for (int i = 0; i < cnt; i++) {
            TestBShardEntity b = entityManager.newEntity(TestBShardEntity.class);

//            TestBShardEntity b = testBShardEntityRepository.factory();



            b.setA(aList.get(i % 10));

            b.setValue(prefix + "B");
            b.setNewValue(prefix + "newB" + i);
            b.setExecuteTime(LocalDateTime.now());

            List<TestCShardEntity> cEntities = new ArrayList<>();
            for (int j = 0; j < cntArray; j++) {
                TestCShardEntity c = entityManager.newEntity(TestCShardEntity.class);
                c.setValue(prefix + "C");
                c.setNewValue(prefix + "newC" + (i * cntArray + j));
                c.setExecuteTime(LocalDateTime.now());
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
    public List<TestOtherShardEntity> generateOther(int cnt) {
        List<TestOtherShardEntity> entities = new ArrayList<>();
        for (int i = 0; i < cnt; i++) {
            TestOtherShardEntity entity = entityManager.newEntity(TestOtherShardEntity.class);

            entity.setABoolean(true);

            entity.setAByte((byte) 1);
            entity.setADouble(2d);
            entity.setAFloat(3f);
            entity.setAShort((short) 4);
            entity.setBigDecimal(BigDecimal.valueOf(3.14));
            entity.setInteger(7);
            try {
                entity.setBlob(new SerialBlob("TEST BLOB".getBytes()));
                entity.setClob(new SerialClob("TEST CLOB".toCharArray()));
            } catch (SQLException err) {
                throw new RuntimeException(err);
            }
            entity.setExecuteTime(new Date());
            entity.setLocalDateTime(LocalDateTime.now());
            entity.setTime(new Time(System.currentTimeMillis()));
            entity.setTimestamp(new Timestamp(System.currentTimeMillis()));

            entity.setStatus(TestStatus.PROCESS);
            try {
                entity.setUrl(new URL("https://www.baeldung.com/java-url"));
            } catch (MalformedURLException err) {
                throw new RuntimeException(err);
            }
            entity.setUid(UUID.randomUUID());

            entities.add(entity);
        }
        return entities;
    }

    @Override
    public void saveOther(List<TestOtherShardEntity> entities) {
        entityManager.saveAll(entities);
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
