package com.antalex.service.impl;

import com.antalex.config.SpringJdbcConfig;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.domain.persistence.entity.hiber.TestAEntity;
import com.antalex.domain.persistence.entity.hiber.TestBEntity;
import com.antalex.domain.persistence.entity.hiber.TestCEntity;
import com.antalex.domain.persistence.repository.TestBRepository;
import com.antalex.service.TestService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
public class TestServiceImpl implements TestService{
    private static final String INS_B_QUERY = "INSERT INTO TEST_B (ID,SHARD_VALUE,C_VALUE,C_NEW_VALUE) VALUES (?,?,?,?)";
    private static final String INS_C_QUERY = "INSERT INTO TEST_C (ID,SHARD_VALUE,C_VALUE,C_NEW_VALUE,C_B_REF) VALUES (?,?,?,?,?)";


    private final DataSource dataSource;
    private final TestBRepository testBRepository;
    private final ShardDataBaseManager databaseManager;

    TestServiceImpl(SpringJdbcConfig springJdbcConfig,
                    TestBRepository testBRepository,
                    ShardDataBaseManager databaseManager)
    {
        this.dataSource = springJdbcConfig.getDataSource();
        this.testBRepository = testBRepository;
        this.databaseManager = databaseManager;
    }

    @Override
    public List<TestBEntity> generate(int cnt, int cntArray, TestAEntity testAEntity, String prefix) {
        List<TestBEntity> bList = new ArrayList<>();;
        for (int i = 0; i < cnt; i++) {
            TestBEntity b = new TestBEntity();
            b.setA(testAEntity);
            b.setShardValue(1L);
            b.setValue(prefix + "B" + i);
            b.setNewValue(prefix + "newB" + i);

            List<TestCEntity> cEntities = new ArrayList<>();
            for (int j = 0; j < cntArray; j++) {
                TestCEntity c = new TestCEntity();
                c.setValue(prefix + "C" + (i * cntArray + j));
                c.setNewValue(prefix + "newC" + (i * cntArray + j));
                c.setShardValue(1L);
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

    @Override
    public void save(List<TestBEntity> entities) {
        try {
            Connection connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            PreparedStatement preparedBStatement = connection.prepareStatement(INS_B_QUERY);
            PreparedStatement preparedCStatement = connection.prepareStatement(INS_C_QUERY);

            entities.forEach(
                    entity -> {
                        try {
                            entity.setId(databaseManager.sequenceNextVal() * 10000L);
                            preparedBStatement.setLong(1, entity.getId());
                            preparedBStatement.setLong(2, entity.getShardValue());
                            preparedBStatement.setString(3, entity.getValue());
                            preparedBStatement.setString(4, entity.getNewValue());
                            preparedBStatement.addBatch();

                            entity.getCList().forEach(cEntity -> {
                                try {
                                    cEntity.setId(databaseManager.sequenceNextVal() * 10000L);
                                    preparedCStatement.setLong(1, cEntity.getId());
                                    preparedCStatement.setLong(2, cEntity.getShardValue());
                                    preparedCStatement.setString(3, cEntity.getValue());
                                    preparedCStatement.setString(4, cEntity.getNewValue());
                                    preparedCStatement.setLong(5, entity.getId());
                                    preparedCStatement.addBatch();

                                } catch (SQLException err) {
                                    throw new RuntimeException(err);
                                }
                            });

                        } catch (SQLException err) {
                            throw new RuntimeException(err);
                        }
                    }
            );

            preparedBStatement.executeBatch();
            preparedCStatement.executeBatch();


            connection.commit();

            connection.close();

        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }
}
