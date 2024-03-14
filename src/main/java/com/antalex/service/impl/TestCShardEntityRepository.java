package com.antalex.service.impl;

import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.StorageContext;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.utils.ShardUtils;
import com.antalex.domain.persistence.entity.shard.TestCShardEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
public class TestCShardEntityRepository {
    private static final ShardType SHARD_TYPE = ShardType.SHARDABLE;
    private static final String INS_QUERY = "INSERT INTO $$$.TEST_C (ID,SHARD_VALUE,C_VALUE,C_NEW_VALUE,C_B_REF) VALUES (?,?,?,?,?)";

    private final ShardDataBaseManager dataBaseManager;
    private final Cluster cluster;

    @Autowired
    TestCShardEntityRepository(ShardDataBaseManager dataBaseManager) {
       this.dataBaseManager = dataBaseManager;
       this.cluster = dataBaseManager.getCluster(String.valueOf("DEFAULT"));
    }

    public TestCShardEntity save(TestCShardEntity entity) {
       setStorage(entity, null, true);
       generateId(entity, true);
       return entity;
   }

    public ShardType getShardType(TestCShardEntity entity) {
       return SHARD_TYPE;
    }

    public Cluster getCluster(TestCShardEntity entity) {
       return cluster;
    }

    public void setDependentStorage(TestCShardEntity entity) {
        String a = INS_QUERY;
    }

    public void generateDependentId(TestCShardEntity entity) {
        String a = INS_QUERY;
    }


    public void generateAllId(Iterable<TestCShardEntity> entities) {
        if (entities == null) {
            return;
        }
        entities.forEach(this::generateId);
    }

    public void generateId(TestCShardEntity entity, boolean isSave) {
        if (entity == null) {
            return;
        }
        if (Objects.isNull(entity.getId())) {
            dataBaseManager.generateId(entity);
            generateDependentId(entity);
        } else {
            if (isSave) {
                generateDependentId(entity);
            }
        }
    }

    public void generateId(TestCShardEntity entity) {
        generateId(entity, false);
    }

    public void setAllStorage(Iterable<TestCShardEntity> entities, ShardInstance parent) {
        if (entities == null) {
            return;
        }
        entities.forEach(entity -> setStorage(entity, parent, false));
    }

    private void setStorage(TestCShardEntity entity, ShardInstance parent, boolean force) {
        if (entity == null) {
            return;
        }
        Cluster cluster = getCluster(entity);
        ShardType shardType = getShardType(entity);
        if (
                Optional.ofNullable(entity.getStorageContext())
                        .map(entityStorage ->
                                Optional.ofNullable(parent)
                                        .map(ShardInstance::getStorageContext)
                                        .filter(it ->
                                                it != entityStorage &&
                                                        shardType != ShardType.REPLICABLE &&
                                                        Objects.nonNull(entityStorage.getShard()) &&
                                                        cluster.getId().equals(it.getCluster().getId()) &&
                                                        dataBaseManager.isEnabled(it.getShard())
                                        )
                                        .map(storage ->
                                                Optional.ofNullable(storage.getShard())
                                                        .map(shard -> {
                                                            storage.setShardMap(
                                                                    ShardUtils.addShardMap(
                                                                            ShardUtils.getShardMap(shard.getId()),
                                                                            entityStorage.getShardMap()
                                                                    )
                                                            );
                                                            return false;
                                                        })
                                                        .orElseGet(() -> {
                                                            storage.setShard(entityStorage.getShard());
                                                            storage.setShardMap(
                                                                    ShardUtils.getShardMap(
                                                                            entityStorage.getShard().getId()
                                                                    )
                                                            );
                                                            return true;
                                                        })
                                        )
                                        .orElseGet(() -> {
                                            if (force) {
                                                setDependentStorage(entity);
                                            }
                                            return false;
                                        })
                        )
                        .orElseGet(() -> {
                            entity.setStorageContext(
                                    Optional.ofNullable(parent)
                                            .map(ShardInstance::getStorageContext)
                                            .filter(it ->
                                                    cluster.getId()
                                                            .equals(it.getCluster().getId()) &&
                                                            shardType != ShardType.REPLICABLE &&
                                                            dataBaseManager.isEnabled(it.getShard())
                                            )
                                            .map(storage ->
                                                    Optional.ofNullable(storage.getShard())
                                                            .map(shard ->
                                                                    StorageContext.builder()
                                                                            .cluster(cluster)
                                                                            .stored(false)
                                                                            .shard(shard)
                                                                            .shardMap(
                                                                                    ShardUtils.getShardMap(
                                                                                            shard.getId()
                                                                                    )
                                                                            )
                                                                            .build()
                                                            )
                                                            .orElse(storage)
                                            )
                                            .orElse(
                                                    StorageContext.builder()
                                                            .cluster(cluster)
                                                            .temporary(true)
                                                            .stored(false)
                                                            .build()
                                            )
                            );
                            setDependentStorage(entity);
                            return false;
                        }))
        {
            parent.setStorageContext(
                    StorageContext.builder()
                            .cluster(parent.getStorageContext().getCluster())
                            .shard(parent.getStorageContext().getShard())
                            .shardMap(parent.getStorageContext().getShardMap())
                            .stored(false)
                            .build()
            );
        }
    }
}

