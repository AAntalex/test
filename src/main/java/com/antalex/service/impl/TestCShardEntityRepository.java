package com.antalex.service.impl;

import com.antalex.db.model.Cluster;
import com.antalex.db.model.StorageAttributes;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.utils.ShardUtils;
import com.antalex.domain.persistence.entity.shard.TestCShardEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

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
            entity.setId(dataBaseManager.generateId(entity.getStorageAttributes()));
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

    public void setAllStorage(Iterable<TestCShardEntity> entities, StorageAttributes storage) {
        if (entities == null) {
            return;
        }
        entities.forEach(entity -> setStorage(entity, storage, false));
    }

    public void setStorage(TestCShardEntity entity, StorageAttributes storage, boolean isSave) {
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

