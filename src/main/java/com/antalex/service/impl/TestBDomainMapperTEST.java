package com.antalex.service.impl;

import com.antalex.db.entity.AttributeStorage;
import com.antalex.db.model.Storage;
import com.antalex.db.model.enums.DataFormat;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.ShardEntityManager;
import com.antalex.db.service.api.DataWrapperFactory;
import com.antalex.domain.persistence.domain.*;
import com.antalex.db.domain.abstraction.Domain;
import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.service.DomainEntityManager;
import com.antalex.db.service.DomainEntityMapper;
import com.antalex.domain.persistence.domain.TestBDomain$Interceptor;
import com.antalex.domain.persistence.entity.shard.TestBShardEntity;

import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TestBDomainMapperTEST {/*  implements DomainEntityMapper<TestBDomain, TestBShardEntity> {
    @Autowired
    private DomainEntityManager domainManager;

    @Autowired
    private ShardEntityManager entityManager;

    @Autowired
    private DataWrapperFactory dataWrapperFactory;

    @Autowired
    private ObjectMapper objectMapper;

    private final ThreadLocal<Map<Long, Domain>> domains = ThreadLocal.withInitial(HashMap::new);
    private final Map<String, Storage> storageMap = new HashMap<>();

    @Autowired
    TestBDomainMapperTEST (ShardDataBaseManager dataBaseManager)
    {
        storageMap.put(
                "TestBDomain",
                Storage
                        .builder()
                        .name("TestBDomain")
                        .shardType(ShardType.SHARDABLE)
                        .cluster(entityManager.getCluster(TestBShardEntity.class))
                        .shardType(entityManager.getShardType(TestBShardEntity.class))
                        .dataFormat(DataFormat.JSON)
                        .build());

        storageMap.put(
                "routingSection",
                Storage
                        .builder()
                        .name("routingSection")
                        .shardType(ShardType.SHARDABLE)
                        .cluster(dataBaseManager.getCluster(String.valueOf("RAW")))
                        .shardType(ShardType.SHARDABLE)
                        .dataFormat(DataFormat.JSON)
                        .build());

    }

    @Override
    public TestBDomain newDomain(TestBShardEntity entity) {
        return new TestBDomain$Interceptor(entity, domainManager);
    }

    @Override
    public Storage getDeclaredStorage(String storageName) {
        return storageMap.get(storageName);
    }

    @Override
    public AttributeStorage mapStorage(TestBDomain domain, String storageName) {

        if (domain.isChanged(storageName)) {

            TestBShardEntity entity = domain.getEntity();
            AttributeStorage attributeStorage = entity.getStorageMap().get(storageName);
            if (attributeStorage == null) {
                Storage storage = storageMap.get(storageName);
                attributeStorage = entityManager.newEntity(AttributeStorage.class);
                attributeStorage.setStorageName(storageName);
                attributeStorage.setDataWrapper(dataWrapperFactory.createDataWraper(attributeStorage.getDataFormat()));
                attributeStorage.setCluster(storage.getCluster());
                attributeStorage.setShardType(storage.getShardType());
                entity.getStorageMap().put(storageName, attributeStorage);
            }

            if (attributeStorage.getDataFormat() == DataFormat.JSON) {
                try {
                    ObjectNode root = (ObjectNode) objectMapper.readTree(attributeStorage.getData());
                    root.putPOJO("note", domain.getNote());
                    root.putPOJO("dateProc", domain.getDateProc());
                } catch (JsonProcessingException err) {
                    throw new RuntimeException(err);
                }
            }
        }

        domain.dropChanges();
        return attributeStorage;
    }

    @Override
    public TestBShardEntity map(TestBDomain domain) {
        TestBShardEntity entity = domain.getEntity();
        if (domain.isChanged(1)) {
            entity.setValue(domain.getValue());
        }
        if (domain.isChanged(2)) {
            entity.setNewValue(domain.getNewValue());
        }
        if (domain.isChanged(3)) {
            entity.setExecuteTime(domain.getExecuteTime());
        }
        if (domain.isChanged(4)) {
            entity.setA(domainManager.map(TestADomain.class, domain.getTestA()));
        }
        entity.setCList(domainManager.mapAllToEntities(TestCDomain.class, domain.getTestList()));
        domain.dropChanges();
        return entity;
    }

    @Override
    public TestBDomain map(TestBShardEntity entity) {
        if (!Optional.ofNullable(entity).map(ShardInstance::getId).isPresent()) {
            return null;
        }
        TestBDomain domain = (TestBDomain) domains.get().get(entity.getId());
        if (Objects.isNull(domain)) {
            domain = newDomain(entity);
            domains.get().put(entity.getId(), domain);
        }
        domain.setLazy(true);
        return domain;
    }
*/
}
