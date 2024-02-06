package com.antalex.mapper;

import com.antalex.domain.persistence.entity.shard.TestAShardEntity;
import com.antalex.domain.persistence.entity.shard.TestBShardEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TestConverter implements DtoConverter<TestAShardEntity, TestBShardEntity> {
    private DtoMapper dtoMapper;

    @Autowired
    TestConverter(DtoMapper dtoMapper) {
        this.dtoMapper = dtoMapper;
    }

    @Override
    public TestBShardEntity convert(TestAShardEntity entity) {
        if (entity == null) return null;
        return new TestBShardEntity();
    }
}
