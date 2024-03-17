package com.antalex.service.impl;

import com.antalex.db.model.Cluster;
import com.antalex.db.model.StorageContext;
import com.antalex.db.model.enums.QueryStrategy;
import com.antalex.db.model.enums.QueryType;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.ShardEntityManager;
import com.antalex.db.service.ShardEntityRepository;
import com.antalex.db.service.api.TransactionalQuery;
import com.antalex.domain.persistence.entity.shard.TestCShardEntity;
import com.antalex.domain.persistence.entity.shard.TestCShardEntityInterceptor$;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class TestCShardEntity$RepositoryImpl2 implements ShardEntityRepository<TestCShardEntity> {
    private static final ShardType SHARD_TYPE = ShardType.SHARDABLE;
    private static final String INS_QUERY = "INSERT INTO $$$.TEST_C (SN,ST,SHARD_VALUE,C_VALUE,C_NEW_VALUE,C_B_REF,ID) VALUES (0,?,?,?,?,?,?)";
    private static final String UPD_QUERY = "UPDATE $$$.TEST_C SET SN=SN+1,ST=?,SHARD_VALUE=?,C_VALUE=?,C_NEW_VALUE=?,C_B_REF=? WHERE ID=?";
    private static final String LOCK_QUERY = "SELECT ID FROM $$$.TEST_C WHERE ID=? FOR UPDATE NOWAIT";

    private static final String SELECT_QUERY = "SELECT SHARD_VALUE,C_VALUE,C_NEW_VALUE,C_B_REF FROM $$$.TEST_C WHERE ID=?";

    @Autowired
    private ShardEntityManager entityManager;
    private final Cluster cluster;

    @Autowired
    TestCShardEntity$RepositoryImpl2(ShardDataBaseManager dataBaseManager) {
        this.cluster = dataBaseManager.getCluster(String.valueOf("DEFAULT"));
    }

    @Override
    public TestCShardEntity newEntity() {
        return new TestCShardEntityInterceptor$();
    }

    @Override
    public TestCShardEntity newEntity(Long id, StorageContext storageContext) {
        TestCShardEntity entity = new TestCShardEntityInterceptor$();
        entity.setId(id);
        entity.setStorageContext(storageContext);
        return entity;
    }

    @Override
    public ShardType getShardType() {
        return SHARD_TYPE;
    }

    @Override
    public Cluster getCluster() {
        return cluster;
    }

    @Override
    public void setDependentStorage(TestCShardEntity entity) {
    }

    @Override
    public void persist(TestCShardEntity entity) {
        entityManager
                .createQueries(entity, entity.isStored() ? UPD_QUERY : INS_QUERY, QueryType.DML)
                .forEach(query ->
                        query
                                .bind(entityManager.getTransactionUUID())
                                .bind(entity.getStorageContext().getShardMap())
                                .bind(entity.getValue())
                                .bind(entity.getNewValue())
                                .bind(entity.getB())
                                .bind(entity.getId())
                                .addBatch()
                );
        additionalPersist(entity);
    }

    @Override
    public void generateDependentId(TestCShardEntity entity) {
    }

    @Override
    public void lock(TestCShardEntity entity) {
        entityManager
                .createQuery(entity, LOCK_QUERY, QueryType.LOCK, QueryStrategy.OWN_SHARD)
                .bind(entity.getId())
                .execute();
    }

    @Override
    public TestCShardEntity find(Long id, StorageContext storageContext) {
        return find(newEntity(id, storageContext));
    }

    @Override
    public TestCShardEntity find(TestCShardEntity entity) {
        try {
            ResultSet resultSet = (ResultSet) entityManager
                    .createQuery(entity, SELECT_QUERY, QueryType.SELECT, QueryStrategy.OWN_SHARD)
                    .bind(entity.getId())
                    .getResult();
            if (resultSet.next()) {
                entity.getStorageContext().setShardMap(resultSet.getLong(1));
                entity.setValue((String) resultSet.getObject(2));
                entity.setNewValue((String) resultSet.getObject(3));
                entity.setB((Long) resultSet.getObject(4));
                entity.getStorageContext().setLazy(false);
            } else {
                return null;
            }
        } catch (SQLException err) {
            throw new RuntimeException(err);
        }
        return entity;
    }

    private void additionalPersist(TestCShardEntity entity) {
        if (entity.hasNewShards()) {
            entityManager
                    .createQueries(entity, INS_QUERY, QueryType.DML, QueryStrategy.NEW_SHARDS)
                    .forEach(query ->
                            query
                                    .bind(entityManager.getTransactionUUID())
                                    .bind(entity.getStorageContext().getShardMap())
                                    .bind(entity.getValue())
                                    .bind(entity.getNewValue())
                                    .bind(entity.getB())
                                    .bind(entity.getId())
                                    .addBatch()
                    );
        }
    }
}
