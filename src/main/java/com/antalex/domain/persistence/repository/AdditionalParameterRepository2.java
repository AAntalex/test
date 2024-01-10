package com.antalex.domain.persistence.repository;

import com.antalex.db.model.StorageAttributes;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.ShardEntityRepository;
import com.antalex.domain.persistence.entity.AdditionalParameterEntity2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Objects;


@Repository
public class AdditionalParameterRepository2 implements ShardEntityRepository<AdditionalParameterEntity2> {
    private static final String CLUSTER = "DEFAULT";
    private static final ShardType SHARD_TYPE = ShardType.REPLICABLE;
    private static final String INS_QUERY = "INSERT INTO IBS.Z#VND_ADD_PARAMS (ID, SHARD_VALUE, C_PARENT_ID, C_CODE, C_VALUE) VALUES (?, ?, ?, ?, ?)";

    @Autowired
    ShardDataBaseManager dataBaseManager;

    @Override
    public AdditionalParameterEntity2 save(AdditionalParameterEntity2 entity) {
        if (Objects.isNull(entity.getId())) {
            StorageAttributes storageAttributes = new StorageAttributes();
            storageAttributes.setStored(false);
            storageAttributes.setShardType(SHARD_TYPE);
            storageAttributes.setCluster(dataBaseManager.getCluster(CLUSTER));
            entity.setStorageAttributes(storageAttributes);
            entity.setId(dataBaseManager.generateId(storageAttributes));
        }
        return entity;
    }

    @Override
    public ShardType getShardType(AdditionalParameterEntity2 entity) {
        return SHARD_TYPE;
    }

    public void flush() {

    }
}

