package com.antalex.domain.persistence.repository;

import com.antalex.dao.ShardEntityRepository;
import com.antalex.domain.persistence.entity.AdditionalParameterEntity;
import org.springframework.stereotype.Repository;


@Repository
public class AdditionalParameterRepository2 implements ShardEntityRepository<AdditionalParameterEntity> {

    @Override
    public AdditionalParameterEntity save(AdditionalParameterEntity entity) {

        return null;
    }

    public void flush() {

    }
}

