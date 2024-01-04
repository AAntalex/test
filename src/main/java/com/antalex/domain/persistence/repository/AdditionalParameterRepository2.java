package com.antalex.domain.persistence.repository;

import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.ShardEntityRepository;
import com.antalex.domain.persistence.entity.AdditionalParameterEntity2;
import org.apache.catalina.Store;
import org.springframework.stereotype.Repository;

import javax.persistence.Persistence;
import java.util.Objects;


@Repository
public class AdditionalParameterRepository2 implements ShardEntityRepository<AdditionalParameterEntity2> {
    private static final String CLUSTER = "DEFAULT";
    private static final ShardType SHARD_TYPE = ShardType.REPLICABLE;
    private static final String INS_QUERY = "INSERT INTO IBS.Z#VND_ADD_PARAMS (ID, SHARD_VALUE, C_PARENT_ID, C_CODE, C_VALUE) VALUES (?, ?, ?, ?, ?)";

    @Override
    public AdditionalParameterEntity2 save(AdditionalParameterEntity2 entity) {
        if (Objects.isNull(entity.getId())) {
            
        }
        return null;
    }

    public void flush() {

    }
}

