package com.antalex.service.impl;

import com.antalex.db.model.enums.QueryType;
import com.antalex.db.service.CriteriaCacheManager;
import com.antalex.db.service.CriteriaRepository;
import com.antalex.db.service.ShardEntityManager;
import com.antalex.db.service.api.ResultQuery;
import com.antalex.db.service.impl.managers.TransactionalCacheManager;
import com.antalex.domain.persistence.criteria.MainDocumCriteria;
import com.antalex.domain.persistence.entity.shard.app.MainDocumEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

@Component
public class MainDocumCriteriaRepository implements CriteriaRepository<MainDocumCriteria> {
    private static final String QUERY = "SELECT FROM $$$.T_MAIN_DOCUM (ID,SHARD_MAP,C_VALUE,C_NEW_VALUE) VALUES (?,?,?,?)";

    private final ShardEntityManager entityManager;
    private final CriteriaCacheManager<MainDocumCriteria> cacheManager = new TransactionalCacheManager<>();

    @Autowired
    MainDocumCriteriaRepository(ShardEntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Stream<MainDocumCriteria> get(Object... binds) {
        ResultQuery result = entityManager
                .createQuery(MainDocumEntity.class, QUERY, QueryType.SELECT)
                .bindAll(binds)
                .getResult();
        try {
            while (result.next()) {
                MainDocumCriteria criteria = new MainDocumCriteria();


            }
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
        return Stream.empty();
    }
}

