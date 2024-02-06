package com.antalex.domain.persistence.repository;

import com.antalex.domain.persistence.entity.AdditionalParameterEntity2;
import com.antalex.test.ShardEntityManager2;
import com.antalex.test.ShardEntityRepository2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class AdditionalParameterRepository2 implements ShardEntityRepository2<AdditionalParameterEntity2> {
    private ShardEntityManager2 entityManager;


    @Autowired
    AdditionalParameterRepository2(ShardEntityManager2 entityManager)
    {
        this.entityManager = entityManager;
    }

    @Override
    public AdditionalParameterEntity2 convert(AdditionalParameterEntity2 entity) {
        return null;
    }
}

