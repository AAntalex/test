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
import com.antalex.db.service.impl.transaction.SharedEntityTransaction;
import com.antalex.db.utils.Utils;
import com.antalex.domain.persistence.entity.shard.TestAShardEntity;
import com.antalex.domain.persistence.entity.shard.TestBShardEntity;
import com.antalex.domain.persistence.entity.shard.TestBShardEntity$Interceptor;
import com.antalex.domain.persistence.entity.shard.TestCShardEntity;
import com.antalex.profiler.service.ProfilerService;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.FetchType;
import java.util.*;
import java.util.stream.IntStream;

@Component
public class Test2Repository implements ShardEntityRepository<TestBShardEntity> {
    private static final ShardType SHARD_TYPE = ShardType.MULTI_SHARDABLE;
    private static final String UPD_QUERY_PREFIX = "UPDATE $$$.TEST_B SET SN=SN+1,ST=?,SHARD_MAP=?";
    private static final String INS_QUERY = "INSERT INTO $$$.TEST_B (SN,ST,SHARD_MAP,C_VALUE,C_A_REF,C_NEW_VALUE,C_EXECUTE_TIME,ID) VALUES (0,?,?,?,?,?,?,?)";
    private static final String UPD_QUERY = "UPDATE $$$.TEST_B SET SN=SN+1,ST=?,SHARD_MAP=?,C_VALUE=?,C_A_REF=?,C_NEW_VALUE=?,C_EXECUTE_TIME=? WHERE ID=?";
    private static final String DELETE_QUERY = "DELETE FROM $$$.TEST_B WHERE ID=?";
    private static final String LOCK_QUERY = "SELECT ID FROM $$$.TEST_B WHERE ID=? FOR UPDATE NOWAIT";
    private static final String SELECT_PREFIX = "SELECT x0.ID,x0.SHARD_MAP,x0.C_VALUE,x0.C_A_REF,x0.C_NEW_VALUE,x0.C_EXECUTE_TIME";
    private static final String FROM_PREFIX = " FROM $$$.TEST_B x0";
    private static final List<String> COLUMNS = Arrays.asList(
            "C_VALUE",
            "C_A_REF",
            "C_NEW_VALUE",
            "C_EXECUTE_TIME"
    );
    private static final Map<String, String> FIELD_MAP = ImmutableMap.<String, String>builder()
            .put("value", "C_VALUE")
            .put("a", "C_A_REF")
            .put("newValue", "C_NEW_VALUE")
            .put("executeTime", "C_EXECUTE_TIME")
            .build();

    private Map<Long, String> updateQueries = new HashMap<>();
    private ShardEntityManager entityManager;
    private ShardDataBaseManager dataBaseManager;
    private final Cluster cluster;

    private final TestC2Repository testC2Repository;

    private final ProfilerService profiler;

    @Autowired
    Test2Repository(ShardDataBaseManager dataBaseManager, ProfilerService profiler, TestC2Repository testC2Repository) {
        this.cluster = dataBaseManager.getCluster(String.valueOf("DEFAULT"));
        this.dataBaseManager = dataBaseManager;
        this.profiler = profiler;
        this.testC2Repository = testC2Repository;
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
    public TestBShardEntity newEntity() {
        return new TestBShardEntity$Interceptor();
    }

    @Override
    public TestBShardEntity getEntity(Long id, StorageContext storageContext) {
        TestBShardEntity$Interceptor entity = new TestBShardEntity$Interceptor();
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
    public ShardType getShardType(TestBShardEntity entity) {
        return SHARD_TYPE;
    }

    @Override
    public Cluster getCluster() {
        return cluster;
    }

    @Override
    public Cluster getCluster(TestBShardEntity entity) {
        return Optional.ofNullable(entity).map(ShardInstance::getCluster).orElse(cluster);
    }

    @Override
    public void setDependentStorage(TestBShardEntity entity) {
        entityManager.setStorage(entity.getA(), entity);
        entityManager.setAllStorage(((TestBShardEntity$Interceptor) entity).getCList(false), entity);
    }

    @Override
    public void persist(TestBShardEntity entity, boolean delete, boolean onlyChanged) {
        if (delete) {
            entityManager.persistAll(entity.getCList(), true, false);
            entityManager
                    .createQueries(entity, DELETE_QUERY, QueryType.DML)
                    .forEach(query -> query.bind(entity.getId()).addBatch());
        } else {
            entityManager.persist(entity.getA(), onlyChanged);
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
                                        .bind(entity.getA().getId(), checkChanges && !entity.isChanged(2))
                                        .bind(entity.getNewValue(), checkChanges && !entity.isChanged(3))
                                        .bind(entity.getExecuteTime(), checkChanges && !entity.isChanged(4))
                                        .bind(entity.getId())
                                        .addBatch()
                        );
            }
            additionalPersist(entity);
            entityManager.persistAll(((TestBShardEntity$Interceptor) entity).getCList(false), false, onlyChanged);
        }
    }

    @Override
    public void generateDependentId(TestBShardEntity entity) {
        TestBShardEntity$Interceptor entityInterceptor = (TestBShardEntity$Interceptor) entity;
        entityManager.generateId(entity.getA());
        entityManager.generateAllId(entityInterceptor.getCList(false));
        entityInterceptor.getCList(false)
                .stream()
                .filter(child -> 
                        Optional.ofNullable(child.getB())
                                .map(it -> !it.equals(entity.getId()))
                                .orElse(true)
                )
                .forEach(it -> it.setB(entity.getId()));
    }

    @Override
    public void lock(TestBShardEntity entity) {
        entityManager
                .createQuery(entity, LOCK_QUERY, QueryType.LOCK, QueryStrategy.OWN_SHARD)
                .bind(entity.getId())
                .execute();
    }


    @Override
    public TestBShardEntity extractValues(TestBShardEntity entity, ResultQuery result, int index) {
        try {
            if (!Optional.ofNullable(result.getLong(++index)).map(it -> it == 0L).orElse(true)) {
                if (entity == null) {
                    entity = entityManager.getEntity(TestBShardEntity.class, result.getLong(index));
                }
                TestBShardEntity$Interceptor entityInterceptor = (TestBShardEntity$Interceptor) entity;
                entityInterceptor.setShardMap(result.getLong(++index));
                entityInterceptor.setValue(result.getString(++index), false);
                entityInterceptor.setA(entityManager.getEntity(TestAShardEntity.class, result.getLong(++index)), false);
                entityInterceptor.setNewValue(result.getString(++index), false);
                entityInterceptor.setExecuteTime(result.getOffsetDateTime(++index), false);
                entityInterceptor.getStorageContext().setLazy(false);
                entityInterceptor.init();
            }
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
        return null;
    }

    @Override
    public TestBShardEntity find(TestBShardEntity entity, Map<String, DataStorage> storageMap) {
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
                entity.setCList(entityManager.findAll(TestCShardEntity.class, entity, "x0.C_B_REF=?", entity.getId()));
            } else {
                return null;
            }
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
        return entity;
    }

    @Override
    public TestBShardEntity find(Map<String, DataStorage> storageMap, String condition, Object... binds) {
        try {
            ResultQuery result = entityManager
                    .createQuery(
                            TestBShardEntity.class,
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
                TestBShardEntity entity = entityManager.getEntity(TestBShardEntity.class, result.getLong(1));
                int index = 0;
                extractValues(entity, result, index);
                index = index + 6;
                entity.setAttributeStorage(entityManager.extractAttributeStorage(storageMap, result, cluster, index));
                entity.setCList(entityManager.findAll(TestCShardEntity.class, entity, "x0.C_B_REF=?", entity.getId()));
                return entity;
            } else {
                return null;
            }
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    @Override
    public List<TestBShardEntity>  findAll(
            Map<String, DataStorage> storageMap,
            Integer limit,
            String condition,
            Object... binds)
    {
        return findAll(
                entityManager
                        .createQuery(
                                TestBShardEntity.class, 
                                getSelectQuery(storageMap) +
                                        Optional.ofNullable(Utils.transformCondition(condition, FIELD_MAP))
                                                .map(it -> " and " + it)
                                                .orElse(StringUtils.EMPTY),
                                QueryType.SELECT
                        )
                        .fetchLimit(limit)
                        .bindAll(binds)
                        .getResult(),
                storageMap
        );
    }

    @Override
    public List<TestBShardEntity> findAll(
            Map<String, DataStorage> storageMap,
            List<Long> ids,
            String condition)
    {
        List<TestBShardEntity> result = new ArrayList<>();
        QueryQueue queue = dataBaseManager
                .createQueryQueueByIds(
                        getSelectQuery(storageMap) +
                                " and " +
                                Optional.ofNullable(Utils.transformCondition(condition, FIELD_MAP))
                                        .orElse("x0.ID in (<IDS>)"),
                        ids
                );
        while (true) {
            if (
                    !result.addAll(
                            Optional
                                    .ofNullable(queue.get())
                                    .map(TransactionalQuery::getResult)
                                    .map(it -> findAll(it, storageMap))
                                    .orElse(Collections.emptyList())
                    )
            ) break;
        }
        return result;
    }

    @Override
    public List<TestBShardEntity> skipLocked(
            Integer limit,
            String condition,
            Object... binds) {
        return findAll(
                entityManager
                        .createQuery(
                                TestBShardEntity.class,
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
    public List<TestBShardEntity> findAll(
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

    private void additionalPersist(TestBShardEntity entity) {
        if (entity.hasNewShards()) {
            entityManager
                    .createQueries(entity, INS_QUERY, QueryType.DML, QueryStrategy.NEW_SHARDS)
                    .forEach(query ->
                            query
                                    .bind(entityManager.getTransactionUUID())
                                    .bindShardMap(entity)
                                    .bind(entity.getValue())
                                    .bind(entity.getA().getId())
                                    .bind(entity.getNewValue())
                                    .bind(entity.getExecuteTime())
                                    .bind(entity.getId())
                                    .addBatch()
                    );
        }
    }

    private List<TestBShardEntity> findAll(ResultQuery result, Map<String, DataStorage> storageMap) {
        profiler.startTimeCounter("findAllBLocal", "AAA");
        SharedEntityTransaction transaction = (SharedEntityTransaction) entityManager.getTransaction();
        transaction.begin();
        List<TestBShardEntity> entities = new ArrayList<>();
        try {
            profiler.startTimeCounter("GET_RESULT", "AAA");
            while (result.next()) {
                TestBShardEntity entity = entityManager.getEntity(TestBShardEntity.class, result.getLong(1));
                int index = 0;
                extractValues(entity, result, index);
                index = index + 6;
                entity.setAttributeStorage(entityManager.extractAttributeStorage(storageMap, result, cluster, index));
                entity.getCList().clear();
                entities.add(entity);
            }
            profiler.fixTimeCounter();
            if (!entities.isEmpty()) {
                profiler.startTimeCounter("GET_C", "AAA");
                testC2Repository.setEntityManager(entityManager);
                profiler.startTimeCounter("getCList", "AAA");
                profiler.startTimeCounter("testC2Repository.findAll(IDS)", "AAA");
                List<TestCShardEntity> lc = testC2Repository.findAll(
                                null,
                                entities
                                        .stream()
                                        .map(ShardInstance::getId)
                                        .toList(),
                                "x0.C_B_REF in (<IDS>)"
                        );
                profiler.fixTimeCounter();
                profiler.startTimeCounter("getBList", "AAA");
                List<TestBShardEntity$Interceptor> lB = lc.stream().map(l ->
                        ((TestBShardEntity$Interceptor) entityManager.getEntity(
                                TestBShardEntity.class, l.getB())
                        )).toList();
                profiler.fixTimeCounter();
                profiler.startTimeCounter("addToBList", "AAA");
                for (int i = 0; i < lc.size(); i++) {
                    lB.get(i).getCList(false).add(lc.get(i));
                }
                profiler.fixTimeCounter();
                profiler.fixTimeCounter();
                profiler.fixTimeCounter();
            }
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
        transaction.commit();
        profiler.fixTimeCounter();
        return entities;
    }


    public TestBShardEntity getBEntity(Long id, SharedEntityTransaction transaction) {
//        profiler.startTimeCounter("getBEntity", "AAA");
        if (Optional.ofNullable(id).map(it -> it.equals(0L)).orElse(true)) {
            return null;
        }
//        profiler.startTimeCounter("getPersistentObject", "AAA");
        TestBShardEntity entity = transaction.getPersistentObject(TestBShardEntity.class, id);
//        profiler.fixTimeCounter();
//        profiler.startTimeCounter("getStorageContext", "AAA");
        if (Objects.isNull(entity)) {
            entity = getEntity(id, dataBaseManager.getStorageContext(id));
            transaction.addPersistentObject(TestBShardEntity.class, id, entity);
        }
//        profiler.fixTimeCounter();
//        profiler.fixTimeCounter();
        return entity;
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
