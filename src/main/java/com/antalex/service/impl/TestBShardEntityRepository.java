package com.antalex.service.impl;

import com.antalex.db.model.Cluster;
import com.antalex.db.model.StorageAttributes;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.utils.ShardUtils;
import com.antalex.domain.persistence.entity.shard.TestBShardEntity;
import com.antalex.domain.persistence.entity.shard.TestBShardEntityExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
public class TestBShardEntityRepository {
    private static final ShardType SHARD_TYPE = ShardType.SHARDABLE;
    private static final String INS_QUERY = "INSERT INTO $$$.TEST_B (ID,SHARD_VALUE,C_VALUE,C_A,C_NEW_VALUE,C_C_LIST) VALUES (?,?,?,?,?,?)";

    private final TestCShardEntityRepository testCShardEntityRepository;
    private final TestAShardEntityRepository testAShardEntityRepository;
    private final ShardDataBaseManager dataBaseManager;
    private final Cluster cluster;

    @Autowired
    TestBShardEntityRepository(ShardDataBaseManager dataBaseManager,
                               TestCShardEntityRepository testCShardEntityRepository,
                               TestAShardEntityRepository testAShardEntityRepository) {
       this.dataBaseManager = dataBaseManager;
       this.testCShardEntityRepository = testCShardEntityRepository;
       this.testAShardEntityRepository = testAShardEntityRepository;
       this.cluster = dataBaseManager.getCluster(String.valueOf("DEFAULT"));
    }

    public TestBShardEntity factory() {
        return new TestBShardEntityExt();
    }

    public TestBShardEntity save(TestBShardEntity entity) {
       setStorage(entity, null, true);
       generateId(entity, true);
       return entity;
   }

    public Iterable saveAll(Iterable<TestBShardEntity> entities) {
        if (entities == null) {
            return null;
        }
        entities.forEach(it -> it = save(it));
        return entities;
    }


    public ShardType getShardType(TestBShardEntity entity) {
       return SHARD_TYPE;
    }

    public Cluster getCluster(TestBShardEntity entity) {
       return cluster;
    }

    public void setDependentStorage(TestBShardEntity entity) {
        testAShardEntityRepository.setStorage(entity.getA(), entity.getStorageAttributes(), false);
        testCShardEntityRepository.setAllStorage(entity.getCList(), null);
    }

    public void generateDependentId(TestBShardEntity entity) {
        testAShardEntityRepository.generateId(entity.getA());
        testCShardEntityRepository.generateAllId(entity.getCList());
        entity.getCList().forEach(it ->
                it.setB(entity.getId())
        );
    }

    public void generateId(TestBShardEntity entity, boolean isSave) {
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

    public void setStorage(TestBShardEntity entity, StorageAttributes storage, boolean isSave) {
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

