package com.antalex.service.impl;

import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.StorageContext;
import com.antalex.db.model.enums.QueryStrategy;
import com.antalex.db.model.enums.QueryType;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.ShardEntityManager;
import com.antalex.db.service.ShardEntityRepository;
import com.antalex.db.service.api.ResultQuery;
import com.antalex.db.service.impl.ResultParallelQuery;
import com.antalex.domain.persistence.entity.shard.TestAShardEntity;
import com.antalex.domain.persistence.entity.shard.TestBShardEntity;
import com.antalex.domain.persistence.entity.shard.TestBShardEntityInterceptor$;
import com.antalex.profiler.service.ProfilerService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.IntStream;


@Component
public class TestBShardEntityRepositoryTest {
    private static final ShardType SHARD_TYPE = ShardType.MULTI_SHARDABLE;
    private static final String UPD_QUERY_PREFIX = "UPDATE $$$.TEST_B SET SN=SN+1,ST=?,SHARD_MAP=?";
    private static final String INS_QUERY = "INSERT INTO $$$.TEST_B (SN,ST,SHARD_MAP,C_VALUE,C_A_REF,C_NEW_VALUE,C_EXECUTE_TIME,ID) VALUES (0,?,?,?,?,?,?,?)";
    private static final String UPD_QUERY = "UPDATE $$$.TEST_B SET SN=SN+1,ST=?,SHARD_MAP=?,C_VALUE=?,C_A_REF=?,C_NEW_VALUE=?,C_EXECUTE_TIME=? WHERE ID=?";
    private static final String INS_UNIQUE_FIELDS_QUERY = "INSERT INTO $$$.TEST_B (SN,ST,SHARD_MAP,C_VALUE,C_NEW_VALUE,ID) VALUES (0,?,?,?,?,?)";
    private static final String LOCK_QUERY = "SELECT ID FROM $$$.TEST_B WHERE ID=? FOR UPDATE NOWAIT";
    private static final String SELECT_QUERY = "SELECT x0.ID,x0.SHARD_MAP,x0.C_VALUE,x0.C_A_REF,x0.C_NEW_VALUE,x0.C_EXECUTE_TIME FROM $$$.TEST_B x0 WHERE x0.SHARD_MAP>=0";
    private static final Long UNIQUE_COLUMNS = 5L;

    private static final List<String> COLUMNS = Arrays.asList(
            "C_VALUE",
            "C_A_REF",
            "C_NEW_VALUE",
            "C_EXECUTE_TIME"
    );
    private Map<Long, String> updateQueries = new HashMap<>();

    @Autowired
    private ShardEntityManager entityManager;

    @Autowired
    private ProfilerService profiler;

    private final Cluster cluster;

    @Autowired
    TestBShardEntityRepositoryTest(ShardDataBaseManager dataBaseManager) {
        this.cluster = dataBaseManager.getCluster(String.valueOf("DEFAULT"));
    }

    public void extractValues(TestBShardEntity entity, ResultQuery result, int index) {
        try {
            if (result.getLong(++index) != 0L) {
                TestBShardEntityInterceptor$ entityInterceptor = (TestBShardEntityInterceptor$) entity;
                entity.setShardMap(result.getLong(++index));
                entityInterceptor.setValue(result.getString(++index), false);
                entityInterceptor.setA(entityManager.getEntity(TestAShardEntity.class, result.getLong(++index)), false);
                entityInterceptor.setNewValue(result.getString(++index), false);
                entityInterceptor.setExecuteTime(result.getDate(++index), false);
                entity.getStorageContext().setLazy(false);
                entityInterceptor.init();
            }
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    public List<TestBShardEntity> findAll(String condition, Object... binds) {
        return findAll(
                entityManager
                        .createQuery(
                                TestBShardEntity.class, 
                                SELECT_QUERY +
                                        Optional.ofNullable(condition).map(it -> " and " + it).orElse(StringUtils.EMPTY),
                                QueryType.SELECT
                        )
                        .bindAll(binds)
                        .getResult()
        );
    }

    private List<TestBShardEntity> findAll(ResultQuery result) {
//        ((ResultParallelQuery) result).setProfiler(profiler);
        profiler.startTimeCounter("findAll", "AAA");
        List<TestBShardEntity> entities = new ArrayList<>();
        try {
            while (result.next()) {
                profiler.startTimeCounter("while", "AAA");
                TestBShardEntity entity = entityManager.getEntity(TestBShardEntity.class, result.getLong(1));
                int index = 0;
                profiler.startTimeCounter("extractValues", "AAA");
                extractValues(entity, result, index);
                profiler.fixTimeCounter();
                index = index + 6;
                entities.add(entity);
                profiler.fixTimeCounter();
            }
        } catch (Exception err) {
            throw new RuntimeException(err);
        }

        profiler.fixTimeCounter();
        return entities;
    }
}
