package com.antalex.service.impl;

import com.antalex.db.entity.AttributeStorage;
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
import com.antalex.db.service.api.ResultQuery;
import com.antalex.domain.persistence.entity.shard.TestAShardEntity;
import com.antalex.domain.persistence.entity.shard.TestBShardEntity;
import com.antalex.domain.persistence.entity.shard.TestBShardEntity$Interceptor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.FetchType;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;


@Component
public class TestBShardEntityRepositoryTEST implements ShardEntityRepository<TestBShardEntity> {
    private static final ShardType SHARD_TYPE = ShardType.MULTI_SHARDABLE;
    private static final String UPD_QUERY_PREFIX = "UPDATE $$$.TEST_B SET SN=SN+1,ST=?,SHARD_MAP=?";
    private static final String INS_QUERY = "INSERT INTO $$$.TEST_B (SN,ST,SHARD_MAP,C_VALUE,C_A_REF,C_NEW_VALUE,C_EXECUTE_TIME,ID) VALUES (0,?,?,?,?,?,?,?)";
    private static final String UPD_QUERY = "UPDATE $$$.TEST_B SET SN=SN+1,ST=?,SHARD_MAP=?,C_VALUE=?,C_A_REF=?,C_NEW_VALUE=?,C_EXECUTE_TIME=? WHERE ID=?";
    private static final String INS_UNIQUE_FIELDS_QUERY = "INSERT INTO $$$.TEST_B (SN,ST,SHARD_MAP,C_VALUE,ID) VALUES (0,?,?,?,?)";
    private static final String LOCK_QUERY = "SELECT ID FROM $$$.TEST_B WHERE ID=? FOR UPDATE NOWAIT";
    private static final String SELECT_PREFIX = "SELECT x0.ID,x0.SHARD_MAP,x0.C_VALUE,x0.C_A_REF,x0.C_NEW_VALUE,x0.C_EXECUTE_TIME";
    private static final String FROM_PREFIX = " FROM $$$.TEST_B x0 WHERE x0.SHARD_MAP>=0";

    private static final Long UNIQUE_COLUMNS = 1L;

    private static final List<String> COLUMNS = Arrays.asList(
            "C_VALUE",
            "C_A_REF",
            "C_NEW_VALUE",
            "C_EXECUTE_TIME"
    );
    private Map<Long, String> updateQueries = new HashMap<>();

    private ShardEntityManager entityManager;
    private final Cluster cluster;

    private String getSelectQuery(Map<String, DataStorage> storageMap) {
        if (Objects.nonNull(storageMap)) {
            StringBuilder selectPrefix = new StringBuilder(SELECT_PREFIX);
            StringBuilder fromPrefix = new StringBuilder(FROM_PREFIX);
            int idx = 0;
            for (DataStorage dataStorage : storageMap.values()) {
                if (dataStorage.getFetchType() == FetchType.EAGER && dataStorage.getCluster() == cluster) {
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
            return selectPrefix + fromPrefix.toString() + " WHERE x0.SHARD_MAP>=0 and x0.ID=?";
        } else {
            return SELECT_PREFIX + FROM_PREFIX + " WHERE x0.SHARD_MAP>=0 and x0.ID=?";
        }
    }

    @Autowired
    TestBShardEntityRepositoryTEST(ShardDataBaseManager dataBaseManager) {
        this.cluster = dataBaseManager.getCluster(String.valueOf("DEFAULT"));
    }

    @Override
    public void setEntityManager(ShardEntityManager entityManager) {
        this.entityManager = entityManager;
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
        return cluster;
    }

    @Override
    public void setDependentStorage(TestBShardEntity entity) {
        entityManager.setStorage(entity.getA(), entity);
        entityManager.setAllStorage(entity.getCList(), entity);
    }

    @Override
    public void persist(TestBShardEntity entity, boolean onlyChanged) {
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
        entityManager.persistAll(entity.getCList(), onlyChanged);
    }

    @Override
    public void generateDependentId(TestBShardEntity entity) {
        entityManager.generateId(entity.getA());
        entityManager.generateAllId(entity.getCList());
        entity.getCList()
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
            if (result.getLong(++index) != 0L) {
                TestBShardEntity$Interceptor entityInterceptor = (TestBShardEntity$Interceptor) entity;
                entity.setShardMap(result.getLong(++index));
                entityInterceptor.setValue(result.getString(++index), false);
                entityInterceptor.setA(entityManager.getEntity(TestAShardEntity.class, result.getLong(++index)), false);
                entityInterceptor.setNewValue(result.getString(++index), false);
                entityInterceptor.setExecuteTime(result.getLocalDateTime(++index), false);
                entity.getStorageContext().setLazy(false);
                entityInterceptor.init();
                return entityInterceptor;
            }
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
        return null;
    }
    @Override
    public TestBShardEntity find(TestBShardEntity entity, Map<String, DataStorage> storageMap) {
        try {
            TestBShardEntity$Interceptor entityInterceptor = (TestBShardEntity$Interceptor) entity;
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
                index += 6;
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
    public List<TestBShardEntity> findAll(Map<String, DataStorage> storageMap, String condition, Object... binds) {
        return findAll(
                entityManager
                        .createQuery(
                                TestBShardEntity.class,
                                getSelectQuery(storageMap) +
                                        Optional.ofNullable(condition).map(it -> " and " + it).orElse(StringUtils.EMPTY),
                                QueryType.SELECT
                        )
                        .bindAll(binds)
                        .getResult(),
                storageMap
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
            return findAll(storageMap, condition, binds);
        }
        return findAll(
                entityManager
                        .createQuery(
                                parent,
                                getSelectQuery(storageMap) +
                                        Optional.ofNullable(condition).map(it -> " and " + it).orElse(StringUtils.EMPTY),
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
        boolean isUpdate = entity.isStored();
        if (!entity.hasMainShard() && (!isUpdate || entity.isChanged() && (entity.getChanges() & UNIQUE_COLUMNS) > 0L)) {
            entityManager
                    .createQuery(
                            entity,
                            isUpdate ?
                                    getUpdateSQL(entity.getChanges() & UNIQUE_COLUMNS) :
                                    INS_UNIQUE_FIELDS_QUERY,
                            QueryType.DML,
                            QueryStrategy.MAIN_SHARD
                    )
                    .bind(entityManager.getTransactionUUID())
                    .bindShardMap(entity)
                    .bind(entity.getValue(), isUpdate && !entity.isChanged(1))
                    .bind(entity.getId())
                    .addBatch();
        }
    }

    private List<TestBShardEntity> findAll(ResultQuery result, Map<String, DataStorage> storageMap) {
        List<TestBShardEntity> entities = new ArrayList<>();
        try {
            while (result.next()) {
                TestBShardEntity entity = entityManager.getEntity(TestBShardEntity.class, result.getLong(1));
                int index = 0;
                extractValues(entity, result, index);
                index = index + 6;
                entity.setAttributeStorage(entityManager.extractAttributeStorage(storageMap, result, cluster, index));
                entities.add(entity);
            }
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
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
}
