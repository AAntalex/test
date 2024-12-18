package com.antalex.service.impl;

import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.DataStorage;
import com.antalex.db.model.StorageContext;
import com.antalex.db.model.enums.QueryStrategy;
import com.antalex.db.model.enums.QueryType;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.ShardEntityManager;
import com.antalex.db.service.ShardEntityRepository;
import com.antalex.db.service.api.QueryQueue;
import com.antalex.db.service.api.ResultQuery;
import com.antalex.db.service.api.TransactionalQuery;
import com.antalex.db.utils.Utils;
import com.antalex.domain.persistence.entity.shard.TestCShardEntity;
import com.antalex.domain.persistence.entity.shard.TestCShardEntity$Interceptor;
import com.antalex.profiler.service.ProfilerService;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.FetchType;
import java.util.*;
import java.util.stream.IntStream;

@Component
public class TestC2Repository implements ShardEntityRepository<TestCShardEntity> {
    private static final ShardType SHARD_TYPE = ShardType.SHARDABLE;
    private static final String UPD_QUERY_PREFIX = "UPDATE $$$.TEST_C SET SN=SN+1,ST=?,SHARD_MAP=?";
    private static final String INS_QUERY = "INSERT INTO $$$.TEST_C (SN,ST,SHARD_MAP,C_VALUE,C_NEW_VALUE,C_B_REF,C_EXECUTE_TIME,ID) VALUES (0,?,?,?,?,?,?,?)";
    private static final String UPD_QUERY = "UPDATE $$$.TEST_C SET SN=SN+1,ST=?,SHARD_MAP=?,C_VALUE=?,C_NEW_VALUE=?,C_B_REF=?,C_EXECUTE_TIME=? WHERE ID=?";
    private static final String DELETE_QUERY = "DELETE FROM $$$.TEST_C WHERE ID=?";
    private static final String LOCK_QUERY = "SELECT ID FROM $$$.TEST_C WHERE ID=? FOR UPDATE NOWAIT";
    private static final String SELECT_PREFIX = "SELECT x0.ID,x0.SHARD_MAP,x0.C_VALUE,x0.C_NEW_VALUE,x0.C_B_REF,x0.C_EXECUTE_TIME";
    private static final String FROM_PREFIX = " FROM $$$.TEST_C x0";
    private static final List<String> COLUMNS = Arrays.asList(
            "C_VALUE",
            "C_NEW_VALUE",
            "C_B_REF",
            "C_EXECUTE_TIME"
    );
    private static final Map<String, String> FIELD_MAP = ImmutableMap.<String, String>builder()
            .put("value", "C_VALUE")
            .put("newValue", "C_NEW_VALUE")
            .put("b", "C_B_REF")
            .put("executeTime", "C_EXECUTE_TIME")
            .build();

    private Map<Long, String> updateQueries = new HashMap<>();
    private ShardEntityManager entityManager;
    private ShardDataBaseManager dataBaseManager;
    private final Cluster cluster;

    private final ProfilerService profiler;

    @Autowired
    TestC2Repository(ShardDataBaseManager dataBaseManager, ProfilerService profiler) {
        this.cluster = dataBaseManager.getCluster(String.valueOf("DEFAULT"));
        this.dataBaseManager = dataBaseManager;
        this.profiler = profiler;
    }

    @Override
    public void setEntityManager(ShardEntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Map<String, String> getFieldMap() {
        return FIELD_MAP;
    }

    @Override
    public TestCShardEntity newEntity() {
        return new TestCShardEntity$Interceptor();
    }

    @Override
    public TestCShardEntity getEntity(Long id, StorageContext storageContext) {
        TestCShardEntity$Interceptor entity = new TestCShardEntity$Interceptor();
        entity.setId(id);
        entity.setStorageContext(storageContext);
        entity.setEntityManager(entityManager);
        entity.init();
        return entity;
    }

    @Override
    public ShardType getShardType() {
        return SHARD_TYPE;
    }

    @Override
    public ShardType getShardType(TestCShardEntity entity) {
        return SHARD_TYPE;
    }

    @Override
    public Cluster getCluster() {
        return cluster;
    }

    @Override
    public Cluster getCluster(TestCShardEntity entity) {
        return Optional.ofNullable(entity).map(ShardInstance::getCluster).orElse(cluster);
    }

    @Override
    public void setDependentStorage(TestCShardEntity entity) {
    }

    @Override
    public void persist(TestCShardEntity entity, boolean delete, boolean onlyChanged) {
        if (delete) {
            entityManager
                    .createQueries(entity, DELETE_QUERY, QueryType.DML)
                    .forEach(query -> query.bind(entity.getId()).addBatch());
        } else {
            String sql = entity.isStored() ? (onlyChanged ? getUpdateSQL(entity.getChanges()) : UPD_QUERY) : INS_QUERY;
            if (Objects.nonNull(sql)) {
                boolean checkChanges = onlyChanged && entity.isStored();
                entityManager
                        .createQueries(entity, sql, QueryType.DML)
                        .forEach(query ->
                                query
                                        .bind(entityManager.getTransactionUUID())
                                        .bindShardMap(entity)
                                        .bind(entity.getValue(), checkChanges && !entity.isChanged(1))
                                        .bind(entity.getNewValue(), checkChanges && !entity.isChanged(2))
                                        .bind(entity.getB(), checkChanges && !entity.isChanged(3))
                                        .bind(entity.getExecuteTime(), checkChanges && !entity.isChanged(4))
                                        .bind(entity.getId())
                                        .addBatch()
                        );
            }
            additionalPersist(entity);
        }
    }

    @Override
    public void generateDependentId(TestCShardEntity entity) {
        TestCShardEntity$Interceptor entityInterceptor = (TestCShardEntity$Interceptor) entity;
    }

    @Override
    public void lock(TestCShardEntity entity) {
        entityManager
                .createQuery(entity, LOCK_QUERY, QueryType.LOCK, QueryStrategy.OWN_SHARD)
                .bind(entity.getId())
                .execute();
    }


    @Override
    public TestCShardEntity extractValues(TestCShardEntity entity, ResultQuery result, int index) {
        try {
            if (!Optional.ofNullable(result.getLong(++index)).map(it -> it == 0L).orElse(true)) {
                if (entity == null) {
                    entity = entityManager.getEntity(TestCShardEntity.class, result.getLong(index));
                }
                TestCShardEntity$Interceptor entityInterceptor = (TestCShardEntity$Interceptor) entity;
                entityInterceptor.setShardMap(result.getLong(++index));
                entityInterceptor.setValue(result.getString(++index), false);
                entityInterceptor.setNewValue(result.getString(++index), false);
                entityInterceptor.setB(result.getLong(++index), false);
                entityInterceptor.setExecuteTime(result.getLocalDateTime(++index), false);
                entityInterceptor.getStorageContext().setLazy(false);
                entityInterceptor.init();
            }
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
        return null;
    }

    @Override
    public TestCShardEntity find(TestCShardEntity entity, Map<String, DataStorage> storageMap) {
        try {
            ResultQuery result = entityManager
                    .createQuery(
                            entity,
                            getSelectQuery(storageMap) + " and x0.ID=?",
                            QueryType.SELECT,
                            QueryStrategy.OWN_SHARD
                    )
                    .bind(entity.getId())
                    .getResult();
            if (result.next()) {
                int index = 0;
                extractValues(entity, result, index);
                index = index + 6;
                entity.setAttributeStorage(entityManager.extractAttributeStorage(storageMap, result, cluster, index));
            } else {
                return null;
            }
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
        return entity;
    }

    @Override
    public TestCShardEntity find(Map<String, DataStorage> storageMap, String condition, Object... binds) {
        try {
            ResultQuery result = entityManager
                    .createQuery(
                            TestCShardEntity.class,
                            getSelectQuery(storageMap) +
                                    Optional.ofNullable(Utils.transformCondition(condition, FIELD_MAP))
                                            .map(it -> " and " + it)
                                            .orElse(StringUtils.EMPTY),
                            QueryType.SELECT
                    )
                    .fetchLimit(1)
                    .bindAll(binds)
                    .getResult();
            if (result.next()) {
                TestCShardEntity entity = entityManager.getEntity(TestCShardEntity.class, result.getLong(1));
                int index = 0;
                extractValues(entity, result, index);
                index = index + 6;
                entity.setAttributeStorage(entityManager.extractAttributeStorage(storageMap, result, cluster, index));
                return entity;
            } else {
                return null;
            }
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    @Override
    public List<TestCShardEntity>  findAll(
            Map<String, DataStorage> storageMap,
            Integer limit,
            String condition,
            Object... binds)
    {
        profiler.startTimeCounter("findAllC", "AAA");
        profiler.startTimeCounter("RUN QUERY", "AAA");
        profiler.startTimeCounter("createQuery", "AAA");
        TransactionalQuery query = entityManager
                .createQuery(
                        TestCShardEntity.class,
                        getSelectQuery(storageMap) +
                                Optional.ofNullable(Utils.transformCondition(condition, FIELD_MAP))
                                        .map(it -> " and " + it)
                                        .orElse(StringUtils.EMPTY),
                        QueryType.SELECT
                );
        profiler.fixTimeCounter();
        profiler.startTimeCounter("PREPARE_QUERY", "AAA");
        query
                .fetchLimit(limit)
                .bindAll(binds);
        profiler.fixTimeCounter();
        profiler.startTimeCounter("getResult", "AAA");
        ResultQuery resultQuery = query.getResult();
        profiler.fixTimeCounter();
        profiler.fixTimeCounter();
        profiler.startTimeCounter("findAll", "AAA");
        List<TestCShardEntity> result = findAll(
                resultQuery,
                storageMap
        );
        profiler.fixTimeCounter();
        profiler.fixTimeCounter();
        return result;
    }

    @Override
    public List<TestCShardEntity> findAll(
            Map<String, DataStorage> storageMap,
            List<Long> ids,
            String condition)
    {
        profiler.startTimeCounter("findAllByIds", "AAA");
        List<TestCShardEntity> result = new ArrayList<>();
        profiler.startTimeCounter("createQueryQueueByIds", "AAA");
        QueryQueue queue = dataBaseManager
                .createQueryQueueByIds(
                        getSelectQuery(storageMap) +
                                " and " +
                                Optional.ofNullable(Utils.transformCondition(condition, FIELD_MAP))
                                        .orElse("x0.ID in (<IDS>)"),
                        ids
                );
        profiler.fixTimeCounter();
        while (true) {
            profiler.startTimeCounter("Queue.get", "AAA");
            profiler.startTimeCounter("getResult", "AAA");
            ResultQuery resultQuery = Optional
                    .ofNullable(queue.get())
                    .map(TransactionalQuery::getResult)
                    .orElse(null);
            profiler.fixTimeCounter();
            profiler.startTimeCounter("findAll", "AAA");
            boolean res = !result.addAll(
                    Optional
                            .ofNullable(resultQuery)
                            .map(it -> findAll(it, storageMap))
                            .orElse(Collections.emptyList()));
            profiler.fixTimeCounter();
            profiler.fixTimeCounter();

            if (res) break;
        }
        profiler.fixTimeCounter();
        return result;
    }

    @Override
    public List<TestCShardEntity> skipLocked(
            Integer limit,
            String condition,
            Object... binds) {
        return findAll(
                entityManager
                        .createQuery(
                                TestCShardEntity.class,
                                getSelectQuery(null) +
                                        Optional.ofNullable(Utils.transformCondition(condition, FIELD_MAP))
                                                .map(it -> " and " + it)
                                                .orElse(StringUtils.EMPTY) +
                                " FOR UPDATE SKIP LOCKED",
                                QueryType.LOCK
                        )
                        .fetchLimit(limit)
                        .bindAll(binds)
                        .getResult(),
                null
        );
    }

    @Override
    public List<TestCShardEntity> findAll(
            ShardInstance parent,
            Map<String, DataStorage> storageMap,
            String condition,
            Object... binds)
    {
        if (parent.getStorageContext().getCluster() != this.cluster) {
            return findAll(storageMap, null, condition, binds);
        }
        return findAll(
                entityManager
                        .createQuery(
                                parent,
                                getSelectQuery(storageMap) +
                                        Optional.ofNullable(Utils.transformCondition(condition, FIELD_MAP))
                                                .map(it -> " and " + it)
                                                .orElse(StringUtils.EMPTY),
                                QueryType.SELECT
                        )
                        .bindAll(binds)
                        .getResult(),
                storageMap
        );
    }

    private void additionalPersist(TestCShardEntity entity) {
        if (entity.hasNewShards()) {
            entityManager
                    .createQueries(entity, INS_QUERY, QueryType.DML, QueryStrategy.NEW_SHARDS)
                    .forEach(query ->
                            query
                                    .bind(entityManager.getTransactionUUID())
                                    .bindShardMap(entity)
                                    .bind(entity.getValue())
                                    .bind(entity.getNewValue())
                                    .bind(entity.getB())
                                    .bind(entity.getExecuteTime())
                                    .bind(entity.getId())
                                    .addBatch()
                    );
        }
    }

    private List<TestCShardEntity> findAll(ResultQuery result, Map<String, DataStorage> storageMap) {
        profiler.startTimeCounter("findAllCLocal", "AAA");
        List<TestCShardEntity> entities = new ArrayList<>();
        try {
            profiler.startTimeCounter("result.next", "AAA");
            while (result.next()) {
                profiler.startTimeCounter("getEntity", "AAA");
                TestCShardEntity entity = entityManager.getEntity(TestCShardEntity.class, result.getLong(1));
                profiler.fixTimeCounter();
                profiler.startTimeCounter("extractValues", "AAA");
                int index = 0;
                extractValues(entity, result, index);
                profiler.fixTimeCounter();
                profiler.startTimeCounter("add", "AAA");
                index = index + 6;
                entity.setAttributeStorage(entityManager.extractAttributeStorage(storageMap, result, cluster, index));
                entities.add(entity);
                profiler.fixTimeCounter();
            }
            profiler.fixTimeCounter();
            if (!entities.isEmpty()) {
            }
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
        profiler.fixTimeCounter();
        return entities;
    }

    private String getUpdateSQL(Long changes) {
        if (
                Optional.ofNullable(changes)
                .map(it -> it.equals(0L) && COLUMNS.size() <= Long.SIZE)
                .orElse(true)) 
        {
            return null;
        }
        String sql = updateQueries.get(changes);
        if (Objects.isNull(sql)) {
            sql = IntStream.range(0, COLUMNS.size())
                    .filter(idx -> idx > Long.SIZE || (changes & (1L << idx)) > 0L)
                    .mapToObj(idx -> "," + COLUMNS.get(idx) + "=?")
                    .reduce(UPD_QUERY_PREFIX, String::concat) + " WHERE ID=?";
            updateQueries.put(changes, sql);
        }
        return sql;
    }

    private String getSelectQuery(Map<String, DataStorage> storageMap) {
        if (Objects.nonNull(storageMap)) {
            StringBuilder selectPrefix = new StringBuilder(SELECT_PREFIX);
            StringBuilder fromPrefix = new StringBuilder(FROM_PREFIX);
            int idx = 0;
            for (DataStorage dataStorage : storageMap.values()) {
            if (
                    dataStorage.getFetchType() == FetchType.EAGER &&
                            Optional.ofNullable(dataStorage.getCluster())
                                    .map(it -> it == cluster)
                                    .orElse(true)
            ) {
                    idx++;
                    selectPrefix
                            .append(",s").append(idx)
                            .append(".ID,s").append(idx)
                            .append(".SHARD_MAP,s").append(idx)
                            .append(".C_ENTITY_ID,s").append(idx)
                            .append(".C_STORAGE_NAME,s").append(idx)
                            .append(".C_DATA,s").append(idx)
                            .append(".C_DATA_FORMAT");
                    fromPrefix
                            .append(" LEFT OUTER JOIN $$$.APP_ATTRIBUTE_STORAGE s").append(idx)
                            .append(" ON s").append(idx)
                            .append(".C_ENTITY_ID=x0.ID and s").append(idx)
                            .append(".C_STORAGE_NAME='").append(dataStorage.getName()).append("'");
                }
            }
            return selectPrefix + fromPrefix.toString() + " WHERE x0.SHARD_MAP>=0";
        } else {
            return SELECT_PREFIX + FROM_PREFIX + " WHERE x0.SHARD_MAP>=0";
        }
    }

}
