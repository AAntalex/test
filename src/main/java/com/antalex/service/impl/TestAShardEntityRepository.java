package com.antalex.service.impl;

import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.StorageContext;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.utils.ShardUtils;
import com.antalex.domain.persistence.entity.shard.TestAShardEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
public class TestAShardEntityRepository {
    private static final ShardType SHARD_TYPE = ShardType.SHARDABLE;
    private static final String INS_QUERY = "INSERT INTO $$$.TEST_A (ID,SHARD_VALUE,C_VALUE,C_NEW_VALUE) VALUES (?,?,?,?)";

    private final ShardDataBaseManager dataBaseManager;
    private final Cluster cluster;

    @Autowired
    TestAShardEntityRepository(ShardDataBaseManager dataBaseManager) {
        this.dataBaseManager = dataBaseManager;
       this.cluster = dataBaseManager.getCluster(String.valueOf("DEFAULT"));
    }

    public TestAShardEntity save(TestAShardEntity entity) {
       setStorage(entity, null, true);
       generateId(entity, true);
       return entity;
   }

    public ShardType getShardType(TestAShardEntity entity) {
       return SHARD_TYPE;
    }

    public Cluster getCluster(TestAShardEntity entity) {
       return cluster;
    }

    public void setDependentStorage(TestAShardEntity entity) {
    }

    public void generateDependentId(TestAShardEntity entity) {
    }

    public void generateAllId(Iterable<TestAShardEntity> entities) {
        if (entities == null) {
            return;
        }
        entities.forEach(this::generateId);
    }

    public void generateId(TestAShardEntity entity, boolean isSave) {
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

    public void generateId(TestAShardEntity entity) {
        generateId(entity, false);
    }

    public void setStorage(TestAShardEntity entity, ShardInstance parent, boolean force) {
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
                                                            storage.setShardValue(
                                                                    ShardUtils.addShardValue(
                                                                            ShardUtils.getShardValue(shard.getId()),
                                                                            entityStorage.getShardValue()
                                                                    )
                                                            );
                                                            return false;
                                                        })
                                                        .orElseGet(() -> {
                                                            storage.setShard(entityStorage.getShard());
                                                            storage.setShardValue(
                                                                    ShardUtils.getShardValue(
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
                                                                            .shardValue(
                                                                                    ShardUtils.getShardValue(
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
                            .shardValue(parent.getStorageContext().getShardValue())
                            .stored(false)
                            .build()
            );
        }
    }

}

