package com.antalex.service.mapper;

import com.antalex.domain.persistence.entity.hiber.TestBEntity;
import org.apache.ibatis.annotations.*;

@Mapper
public interface EntityMapper {
    @Insert("Insert into ${tablename} " +
            "(ID," +
            "SHARD_MAP," +
            "C_VALUE," +
            "C_A_REF," +
            "C_NEW_VALUE" +
            ") " +
            "values" +
            "(#{entity.id}," +
            "#{entity.shardMap}," +
            "#{entity.value}," +
            "#{entity.a}," +
            "#{entity.newValue}" +
            ")"
    )
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    void insert(@Param("tablename") String tableName,
                @Param("entity") TestBEntity entity) throws Exception;
}
