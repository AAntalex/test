package com.antalex.service.impl;

import com.antalex.config.SpringJdbcConfig;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.domain.persistence.entity.hiber.TestAEntity;
import com.antalex.domain.persistence.entity.hiber.TestBEntity;
import com.antalex.domain.persistence.entity.hiber.TestCEntity;
import com.antalex.domain.persistence.repository.TestBRepository;
import com.antalex.domain.persistence.repository.TestCRepository;
import com.antalex.profiler.service.ProfilerService;
import com.antalex.service.mapper.EntityMapper;
import com.antalex.service.TestService;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class TestServiceImpl implements TestService{
    private static final String INS_B_QUERY = "INSERT INTO TEST_B (ID,SHARD_MAP,C_VALUE,C_NEW_VALUE) VALUES (?,?,?,?)";
    private static final String INS_C_QUERY = "INSERT INTO TEST_C (ID,SHARD_MAP,C_VALUE,C_NEW_VALUE,C_B_REF) VALUES (?,?,?,?,?)";


    private final DataSource dataSource;
    private final ShardDataBaseManager databaseManager;
    private final TestBRepository testBRepository;
    private final SqlSessionFactory sqlSessionFactory;

    @Autowired
    private ProfilerService profiler;
    @Autowired
    private TestCRepository testCRepository;

    TestServiceImpl(ShardDataBaseManager databaseManager,
                    TestBRepository testBRepository,
                    SqlSessionFactory sqlSessionFactory)
    {
        this.dataSource = databaseManager.getDataSource();
        this.databaseManager = databaseManager;
        this.testBRepository = testBRepository;
        this.sqlSessionFactory = sqlSessionFactory;
    }

    @Override
    public List<TestBEntity> generate(int cnt, int cntArray, TestAEntity testAEntity, String prefix) {
        List<TestBEntity> bList = new ArrayList<>();

        List<TestAEntity> aList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TestAEntity a = new TestAEntity();
            a.setId(databaseManager.sequenceNextVal() * 10000L);
            a.setValue(prefix + "A");
            a.setNewValue(prefix + "newA" + i);
            a.setExecuteTime(new Date());
            aList.add(a);
        }

        for (int i = 0; i < cnt; i++) {
            TestBEntity b = new TestBEntity();

            if ("Statement".equals(prefix)) {
                b.setA(aList.get(i % 10));
            }

            b.setShardMap(1L);
            b.setValue(prefix + "B");
            b.setNewValue(prefix + "newB" + i);
            b.setExecuteTime(new Date());

            List<TestCEntity> cEntities = new ArrayList<>();
            for (int j = 0; j < cntArray; j++) {
                TestCEntity c = new TestCEntity();
                c.setValue(prefix + "C");
                c.setNewValue(prefix + "newC" + (i * cntArray + j));
                c.setShardMap(1L);
                c.setExecuteTime(new Date());
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

    @Override
    public void saveMyBatis(List<TestBEntity> testBEntities) {
        profiler.startTimeCounter("Prepare saveMyBatis", "AAA");
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
        try {
            EntityMapper entityMapper = sqlSession.getMapper(EntityMapper.class);
            testBEntities.forEach(
                    entity -> {
                        try {
                            entity.setId(databaseManager.sequenceNextVal() * 10000L);
                            entityMapper.insert("TEST_B", entity);
                        } catch (Exception err) {
                            throw new RuntimeException(err);
                        }
                    }
            );

            testBEntities.forEach(
                    entity ->
                            entity.getCList().forEach(cEntity -> {
                                try {
                                    cEntity.setId(databaseManager.sequenceNextVal() * 10000L);
                                    cEntity.setB(entity.getId());
                                    entityMapper.insertC("TEST_C", cEntity);
                                } catch (Exception err) {
                                    throw new RuntimeException(err);
                                }
                            })
            );
            profiler.fixTimeCounter();
            profiler.startTimeCounter("commit saveMyBatis", "AAA");
            sqlSession.commit();
            profiler.fixTimeCounter();
        } finally {
            sqlSession.close();
        }
    }


    @Transactional
    @Override
    public void saveTransactionalJPA(List<TestBEntity> testBEntities) {
        saveJPA(testBEntities);
    }

    @Override
    public TestBEntity findBByIdMBatis(Long id) {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            EntityMapper entityMapper = sqlSession.getMapper(EntityMapper.class);
            TestBEntity entity =  entityMapper.findById("TEST_B", id);
            entity.setCList(findAllCMBatis(id, entityMapper));
            return entity;
        } catch (Exception err) {
            throw new RuntimeException(err);
        } finally {
            sqlSession.close();
        }
    }

    @Override
    public List<TestBEntity> findAllByValueLikeMBatis(String value) {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            EntityMapper entityMapper = sqlSession.getMapper(EntityMapper.class);
            List<TestBEntity> entities =  entityMapper.findAllByValueLike("TEST_B", value);
//            entities.forEach(b -> b.setCList(findAllCMBatis(b.getId(), entityMapper)));
            return entities;
        } catch (Exception err) {
            throw new RuntimeException(err);
        } finally {
            sqlSession.close();
        }
    }

    @Override
    public List<TestCEntity> findAllCMBatis(Long id, EntityMapper entityMapper) {
        try {
            return entityMapper.findAllC("TEST_C", id);
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    @Override
    public List<TestBEntity> findAllB(String value, EntityMapper entityMapper) {
        try {
            if (entityMapper == null) {
                SqlSession sqlSession = sqlSessionFactory.openSession();
                entityMapper = sqlSession.getMapper(EntityMapper.class);
            }
            return entityMapper.findAllB("TEST_B", value);
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    @Override
    public TestBEntity findBByIdStatement(Long id) {
        try {
            Connection connection = dataSource.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT x0.ID,x0.SHARD_MAP,x0.C_VALUE,x0.C_A_REF,x0.C_NEW_VALUE,x0.C_EXECUTE_TIME FROM TEST_B x0 WHERE x0.SHARD_MAP>=0 and x0.ID=?");
            PreparedStatement preparedStatementC = connection.prepareStatement("SELECT x0.ID,x0.SHARD_MAP,x0.C_VALUE,x0.C_NEW_VALUE,x0.C_B_REF,x0.C_EXECUTE_TIME FROM TEST_C x0 WHERE x0.SHARD_MAP>=0 and x0.C_B_REF=?");

            preparedStatement.setLong(1, id);

            ResultSet result = preparedStatement.executeQuery();
            try {
                if (result.next()) {
                    TestBEntity b = new TestBEntity();
                    b.setId(result.getLong(1));
                    b.setShardMap(result.getLong(2));
                    b.setValue(result.getString(3));
                    b.setNewValue(result.getString(5));
                    b.setExecuteTime(result.getDate(6));
                    b.setCList(findAllCStatement(b.getId(), preparedStatementC));
                    return b;
                }
            } finally {
                connection.close();
            }
            return null;
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    @Override
    public List<TestBEntity> findAllBByValueLikeStatement(String value) {
        try {
            Connection connection = dataSource.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT x0.ID,x0.SHARD_MAP,x0.C_VALUE,x0.C_A_REF,x0.C_NEW_VALUE,x0.C_EXECUTE_TIME FROM TEST_B x0 WHERE x0.SHARD_MAP>=0 and x0.C_VALUE like ?");
            PreparedStatement preparedStatementC = connection.prepareStatement("SELECT x0.ID,x0.SHARD_MAP,x0.C_VALUE,x0.C_NEW_VALUE,x0.C_B_REF,x0.C_EXECUTE_TIME FROM TEST_C x0 WHERE x0.SHARD_MAP>=0 and x0.C_B_REF=?");

            System.out.println("preparedStatement.getFetchSize() = " + preparedStatement.getFetchSize());


            preparedStatement.setString(1, value);

            List<TestBEntity> entities = new ArrayList<>();
            ResultSet result = preparedStatement.executeQuery();
            while (result.next()) {
                TestBEntity b = new TestBEntity();
                b.setId(result.getLong(1));
                b.setShardMap(result.getLong(2));
                b.setValue(result.getString(3));
                b.setNewValue(result.getString(5));
                b.setExecuteTime(result.getDate(6));
/*
                b.setCList(
                        findAllCStatement(
                                b.getId(),
                                preparedStatementC
                        )
                );
*/
                entities.add(b);
            }
            connection.close();
            return entities;
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    @Override
    public List<TestCEntity> findAllCStatement(Long id, PreparedStatement preparedStatement) {
        List<TestCEntity> ret = new ArrayList<>();
        try {
            preparedStatement.setLong(1, id);

            ResultSet result = preparedStatement.executeQuery();
            while (result.next()) {
                TestCEntity c = new TestCEntity();
                c.setId(result.getLong(1));
                c.setShardMap(result.getLong(2));
                c.setValue(result.getString(3));
                c.setNewValue(result.getString(4));
                c.setB(result.getLong(5));
                c.setExecuteTime(result.getDate(6));
                ret.add(c);
            }
            return ret;
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }


    @Override
    public void save(List<TestBEntity> entities) {
        try {
            profiler.startTimeCounter("prepare save", "AAA");
            Connection connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            PreparedStatement preparedBStatement = connection.prepareStatement(INS_B_QUERY);
            PreparedStatement preparedCStatement = connection.prepareStatement(INS_C_QUERY);

            entities.forEach(
                    entity -> {
                        try {
                            entity.setId(databaseManager.sequenceNextVal() * 10000L);
                            preparedBStatement.setLong(1, entity.getId());
                            preparedBStatement.setLong(2, entity.getShardMap());
                            preparedBStatement.setString(3, entity.getValue());
                            preparedBStatement.setString(4, entity.getNewValue());
                            preparedBStatement.addBatch();

                            entity.getCList().forEach(cEntity -> {
                                try {
                                    cEntity.setId(databaseManager.sequenceNextVal() * 10000L);
                                    preparedCStatement.setLong(1, cEntity.getId());
                                    preparedCStatement.setLong(2, cEntity.getShardMap());
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
            profiler.fixTimeCounter();

            profiler.startTimeCounter("executeBatch save", "AAA");
            preparedBStatement.executeBatch();
            preparedCStatement.executeBatch();
            profiler.fixTimeCounter();

            profiler.startTimeCounter("commit save", "AAA");
            connection.commit();
            profiler.fixTimeCounter();


            connection.close();

        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }
}
