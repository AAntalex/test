package com.antalex.domain.persistence.repository;

import com.antalex.db.service.ShardEntityRepository;
import com.antalex.domain.persistence.entity.AdditionalParameterEntity2;
import org.springframework.stereotype.Repository;


@Repository
public class AdditionalParameterRepository2 implements ShardEntityRepository<AdditionalParameterEntity2> {

    @Override
    public AdditionalParameterEntity2 save(AdditionalParameterEntity2 entity) {

        return null;
    }

    public void flush() {

    }
}

