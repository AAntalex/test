package com.antalex.service.impl;

import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.StorageContext;
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
        testAShardEntityRepository.setStorage(entity.getA(), entity, false);
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
            dataBaseManager.generateId(entity);
            generateDependentId(entity);
        } else {
            if (isSave) {
                generateDependentId(entity);
            }
        }
    }

    private void setStorage(TestBShardEntity entity, ShardInstance parent, boolean force) {
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

