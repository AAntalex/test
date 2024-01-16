package com.antalex.domain.persistence.repository;

import com.antalex.db.model.Cluster;
import com.antalex.db.model.StorageAttributes;

import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.ShardEntityRepository;
import com.antalex.domain.persistence.entity.AdditionalParameterEntity2;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;


@Repository
public class AdditionalParameterRepository2 implements ShardEntityRepository<AdditionalParameterEntity2> {
    private static final ShardType SHARD_TYPE = ShardType.REPLICABLE;
    private static final String INS_QUERY = "INSERT INTO IBS.Z#VND_ADD_PARAMS (ID, SHARD_VALUE, C_PARENT_ID, C_CODE, C_VALUE) VALUES (?, ?, ?, ?, ?)";

    private final ShardDataBaseManager dataBaseManager;
    private final Cluster cluster;

    AdditionalParameterRepository2(ShardDataBaseManager dataBaseManager) {
        this.dataBaseManager = dataBaseManager;
        this.cluster = dataBaseManager.getCluster(String.valueOf("DEFAULT"));
    }

    @Override
    public AdditionalParameterEntity2 save(AdditionalParameterEntity2 entity) {
        if (Objects.isNull(entity.getId())) {
            StorageAttributes storageAttributes = new StorageAttributes();
            storageAttributes.setStored(false);
            storageAttributes.setCluster(this.cluster);
            entity.setStorageAttributes(storageAttributes);


            entity.setId(dataBaseManager.generateId(storageAttributes));
        }
        return entity;
    }

    public void setStorage(AdditionalParameterEntity2 entity, StorageAttributes storage) {
        if (Objects.isNull(storage) && Objects.isNull(entity.getStorageAttributes())) {
            storage = new StorageAttributes();
            storage.setStored(false);
            storage.setCluster(this.cluster);
            entity.setStorageAttributes(storage);
        } else {
            if (Objects.isNull(entity.getStorageAttributes())) {
                entity.setStorageAttributes(storage);
            } else {

            }
        }




        if (Objects.isNull(storage) ||
                !Optional.ofNullable(entity.getStorageAttributes())
                        .map(StorageAttributes::getCluster)
                        .orElse(this.cluster)
                        .getId()
                        .equals(storage.getCluster().getId()))
        {
            if (Objects.isNull(entity.getStorageAttributes())) {
                storage = new StorageAttributes();
                storage.setStored(false);
                storage.setCluster(this.cluster);
                entity.setStorageAttributes(storage);
            }
        } else {
            if (
                    Optional.ofNullable(entity.getStorageAttributes())
                            .map(StorageAttributes::getCluster)
                            .orElse(this.cluster)
                            .getId().equals(storage.getCluster().getId()))
            {
                if (Objects.isNull(entity.getStorageAttributes())) {
                    entity.setStorageAttributes(storage);
                } else {
                    if (storage.equals(entity.getStorageAttributes())) {

                    }
                }
            }
        }

    }

    public AdditionalParameterEntity2 save(AdditionalParameterEntity2 entity, StorageAttributes storageAttributes) {
        if (Objects.isNull(entity.getId())) {
            storageAttributes = new StorageAttributes();
            storageAttributes.setStored(false);
            storageAttributes.setCluster(this.cluster);
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

