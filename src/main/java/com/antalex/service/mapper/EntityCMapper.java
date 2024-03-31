package com.antalex.service.mapper;

import com.antalex.domain.persistence.entity.hiber.TestCEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EntityCMapper {
    @Insert("Insert into ${tablename} " +
            "(ID," +
            "SHARD_MAP," +
            "C_VALUE," +
            "C_B_REF," +
            "C_NEW_VALUE" +
            ") " +
            "values" +
            "(#{entity.id}," +
            "#{entity.shardMap}," +
            "#{entity.value}," +
            "#{entity.b}," +
            "#{entity.newValue}" +
            ")"
    )
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    void insert(@Param("tablename") String tableName,
                @Param("entity") TestCEntity entity) throws Exception;
}
