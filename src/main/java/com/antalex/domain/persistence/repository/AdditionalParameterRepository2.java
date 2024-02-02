package com.antalex.domain.persistence.repository;

import com.antalex.db.model.Cluster;

import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.ShardEntityManager;
import com.antalex.db.service.ShardEntityRepository;
import com.antalex.domain.persistence.entity.AdditionalParameterEntity2;
import org.springframework.stereotype.Repository;

import java.util.Objects;


@Repository
public class AdditionalParameterRepository2 implements ShardEntityRepository<AdditionalParameterEntity2> {
    private static final ShardType SHARD_TYPE = ShardType.REPLICABLE;
    private static final String INS_QUERY = "INSERT INTO IBS.Z#VND_ADD_PARAMS (ID, SHARD_VALUE, C_PARENT_ID, C_CODE, C_VALUE) VALUES (?, ?, ?, ?, ?)";

    private final ShardDataBaseManager dataBaseManager;
    private final ShardEntityManager entityManager;
    private final Cluster cluster;

    @Override
    public AdditionalParameterEntity2 save(AdditionalParameterEntity2 entity) {
        if (Objects.isNull(entity.getId())) {
            entityManager.setStorage(entity, null);
            entity.setId(dataBaseManager.generateId(entity));
        }
        return entity;
    }

    AdditionalParameterRepository2(ShardDataBaseManager dataBaseManager,
                                   ShardEntityManager entityManager)
    {
        this.dataBaseManager = dataBaseManager;
        this.entityManager = entityManager;
        this.cluster = dataBaseManager.getCluster(String.valueOf("DEFAULT"));
    }

    @Override
    public Cluster getCluster(AdditionalParameterEntity2 entity) {
        return cluster;
    }

    @Override
    public ShardType getShardType(AdditionalParameterEntity2 entity) {
        return SHARD_TYPE;
    }

    @Override
    public void setDependentStorage(AdditionalParameterEntity2 entity) {

    }

    public void flush() {

    }
}

