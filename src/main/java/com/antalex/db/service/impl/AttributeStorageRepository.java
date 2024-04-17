package com.antalex.db.service.impl;

import com.antalex.db.entity.AttributeStorage;
import com.antalex.db.entity.AttributeStorageInterceptor;
import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.StorageContext;
import com.antalex.db.model.enums.DataFormat;
import com.antalex.db.model.enums.QueryStrategy;
import com.antalex.db.model.enums.QueryType;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.ShardEntityManager;
import com.antalex.db.service.ShardEntityRepository;
import com.antalex.db.service.api.ResultQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.IntStream;


@Component
public class AttributeStorageRepository implements ShardEntityRepository<AttributeStorage> {
    private static final String UPD_QUERY_PREFIX = "UPDATE $$$.APP_ATTRIBUTE_STORAGE SET SN=SN+1,ST=?,SHARD_MAP=?";
    private static final String INS_QUERY = "INSERT INTO $$$.APP_ATTRIBUTE_STORAGE (SN,ST,SHARD_MAP,C_ENTITY_ID,C_STORAGE_NAME,C_DATA,C_DATA_FORMAT,ID) VALUES (0,?,?,?,?,?,?,?)";
    private static final String UPD_QUERY = "UPDATE $$$.APP_ATTRIBUTE_STORAGE SET SN=SN+1,ST=?,SHARD_MAP=?,C_ENTITY_ID=?,C_STORAGE_NAME=?,C_DATA=?,C_DATA_FORMAT=? WHERE ID=?";
    private static final String LOCK_QUERY = "SELECT ID FROM $$$.APP_ATTRIBUTE_STORAGE WHERE ID=? FOR UPDATE NOWAIT";
    private static final String SELECT_QUERY = "SELECT x0.ID,x0.SHARD_MAP,x0.C_ENTITY_ID,x0.C_STORAGE_NAME,x0.C_DATA,x0.C_DATA_FORMAT FROM $$$.APP_ATTRIBUTE_STORAGE x0 WHERE x0.SHARD_MAP>=0";

    private static final List<String> COLUMNS = Arrays.asList(
            "C_ENTITY_ID",
            "C_STORAGE_NAME",
            "C_DATA",
            "C_DATA_FORMAT"
    );
    private Map<Long, String> updateQueries = new HashMap<>();

    @Autowired
    private ShardEntityManager entityManager;

    @Override
    public AttributeStorage newEntity() {
        return new AttributeStorageInterceptor();
    }

    @Override
    public AttributeStorage getEntity(Long id, StorageContext storageContext) {
        AttributeStorageInterceptor entity = new AttributeStorageInterceptor();
        entity.setId(id);
        entity.setStorageContext(storageContext);
        entity.setEntityManager(entityManager);
        entity.init();
        return entity;
    }

    @Override
    public ShardType getShardType(AttributeStorage entity) {
        return entity.getShardType();
    }

    @Override
    public ShardType getShardType() {
        return null;
    }

    @Override
    public Cluster getCluster(AttributeStorage entity) {
        return entity.getCluster();
    }

    @Override
    public Cluster getCluster() {
        return null;
    }

    @Override
    public void setDependentStorage(AttributeStorage entity) {
    }

    @Override
    public void persist(AttributeStorage entity, boolean onlyChanged) {
        String sql = entity.isStored() ? (onlyChanged ? getUpdateSQL(entity.getChanges()) : UPD_QUERY) : INS_QUERY;
        if (Objects.nonNull(sql)) {
            boolean checkChanges = onlyChanged && entity.isStored();
            entityManager
                    .createQueries(entity, sql, QueryType.DML)
                    .forEach(query ->
                            query
                                    .bind(entityManager.getTransactionUUID())
                                    .bindShardMap(entity)
                                    .bind(entity.getEntityId(), checkChanges && !entity.isChanged(1))
                                    .bind(entity.getStorageName(), checkChanges && !entity.isChanged(2))
                                    .bind(entity.getData(), checkChanges && !entity.isChanged(3))
                                    .bind(entity.getDataFormat(), checkChanges && !entity.isChanged(4))
                                    .bind(entity.getId())
                                    .addBatch()
                    );
        }
        additionalPersist(entity);
    }

    @Override
    public void generateDependentId(AttributeStorage entity) {
    }

    @Override
    public void lock(AttributeStorage entity) {
        entityManager
                .createQuery(entity, LOCK_QUERY, QueryType.LOCK, QueryStrategy.OWN_SHARD)
                .bind(entity.getId())
                .execute();
    }


    @Override
    public void extractValues(AttributeStorage entity, ResultQuery result, int index) {
        try {
            if (result.getLong(++index) != 0L) {
                AttributeStorageInterceptor entityInterceptor = (AttributeStorageInterceptor) entity;
                entity.setShardMap(result.getLong(++index));
                entityInterceptor.setEntityId(result.getLong(++index), false);
                entityInterceptor.setStorageName(result.getString(++index), false);
                entityInterceptor.setData(result.getString(++index), false);
                entityInterceptor.setDataFormat(result.getObject(++index, DataFormat.class), false);
                entity.getStorageContext().setLazy(false);
                entityInterceptor.init();
            }
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    @Override
    public AttributeStorage find(AttributeStorage entity) {
        try {
            AttributeStorageInterceptor entityInterceptor = (AttributeStorageInterceptor) entity;
            ResultQuery result = entityManager
                    .createQuery(entity, SELECT_QUERY + " and x0.ID=?", QueryType.SELECT, QueryStrategy.OWN_SHARD)
                    .bind(entity.getId())
                    .getResult();
            if (result.next()) {
                int index = 0;
                extractValues(entity, result, index);
                index = index + 6;
            } else {
                return null;
            }
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
        return entity;
    }

    @Override
    public List<AttributeStorage> findAll(String condition, Object... binds) {
        return findAll(
                entityManager
                        .createQuery(
                                AttributeStorage.class, 
                                SELECT_QUERY +
                                        Optional.ofNullable(condition).map(it -> " and " + it).orElse(StringUtils.EMPTY),
                                QueryType.SELECT
                        )
                        .bindAll(binds)
                        .getResult()
        );
    }

    @Override
    public List<AttributeStorage> findAll(ShardInstance parent, String condition, Object... binds) {
        return findAll(
                entityManager
                        .createQuery(
                                parent,
                                SELECT_QUERY +
                                        Optional.ofNullable(condition).map(it -> " and " + it).orElse(StringUtils.EMPTY),
                                QueryType.SELECT
                        )
                        .bindAll(binds)
                        .getResult()
        );
    }

    private void additionalPersist(AttributeStorage entity) {
        if (entity.hasNewShards()) {
            entityManager
                    .createQueries(entity, INS_QUERY, QueryType.DML, QueryStrategy.NEW_SHARDS)
                    .forEach(query ->
                            query
                                    .bind(entityManager.getTransactionUUID())
                                    .bindShardMap(entity)
                                    .bind(entity.getEntityId())
                                    .bind(entity.getStorageName())
                                    .bind(entity.getData())
                                    .bind(entity.getDataFormat())
                                    .bind(entity.getId())
                                    .addBatch()
                    );
        }
    }

    private List<AttributeStorage> findAll(ResultQuery result) {
        List<AttributeStorage> entities = new ArrayList<>();
        try {
            while (result.next()) {
                AttributeStorage entity = entityManager.getEntity(AttributeStorage.class, result.getLong(1));
                int index = 0;
                extractValues(entity, result, index);
                index = index + 6;
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