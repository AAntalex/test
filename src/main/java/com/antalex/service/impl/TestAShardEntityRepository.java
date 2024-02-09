package com.antalex.service.impl;

import com.antalex.db.model.Cluster;
import com.antalex.db.model.StorageAttributes;
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
            entity.setId(dataBaseManager.generateId(entity.getStorageAttributes()));
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

    public void setAllStorage(Iterable<TestAShardEntity> entities, StorageAttributes storage) {
        if (entities == null) {
            return;
        }
        entities.forEach(entity -> setStorage(entity, storage, false));
    }

    public void setStorage(TestAShardEntity entity, StorageAttributes storage, boolean isSave) {
        if (entity == null) {
            return;
        }
        Cluster cluster = getCluster(entity);
        Optional.ofNullable(entity.getStorageAttributes())
                .map(entityStorage ->
                        Optional.ofNullable(storage)
                                .filter(it ->
                                        it != entityStorage &&
                                                getShardType(entity) != ShardType.REPLICABLE &&
                                                Objects.nonNull(entityStorage.getShard()) &&
                                                cluster.getId().equals(it.getCluster().getId())
                                )
                                .map(it ->
                                        Optional.ofNullable(storage.getShardValue())
                                                .map(shardValue -> {
                                                    storage.setShardValue(
                                                            ShardUtils.addShardValue(
                                                                    shardValue,
                                                                    entityStorage.getShardValue()
                                                            )
                                                    );
                                                    return storage;
                                                })
                                                .orElseGet(() -> {
                                                    storage.setShard(entityStorage.getShard());
                                                    storage.setShardValue(entityStorage.getShardValue());
                                                    return storage;
                                                })
                                )
                                .orElseGet(() -> {
                                    if (isSave) {
                                        setDependentStorage(entity);
                                    }
                                    return entity.getStorageAttributes();
                                })
                )
                .orElseGet(() -> {
                    entity.setStorageAttributes(
                            Optional.ofNullable(storage)
                                    .filter(it ->
                                            cluster.getId()
                                                    .equals(it.getCluster().getId())
                                    )
                                    .orElse(
                                            StorageAttributes.builder()
                                                    .stored(false)
                                                    .cluster(cluster)
                                                    .build()
                                    )
                    );
                    setDependentStorage(entity);
                    return entity.getStorageAttributes();
                });
    }

}

