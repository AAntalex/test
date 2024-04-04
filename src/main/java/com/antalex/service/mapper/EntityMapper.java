package com.antalex.service.mapper;

import com.antalex.domain.persistence.entity.hiber.TestBEntity;
import com.antalex.domain.persistence.entity.hiber.TestCEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface EntityMapper {
    @Insert("Insert into ${tablename} " +
            "(ID," +
            "SHARD_MAP," +
            "C_VALUE," +
            "C_NEW_VALUE," +
            "C_EXECUTE_TIME" +
            ") " +
            "values" +
            "(#{entity.id}," +
            "#{entity.shardMap}," +
            "#{entity.value}," +
            "#{entity.newValue}," +
            "#{entity.executeTime}" +
            ")"
    )
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    void insert(@Param("tablename") String tableName,
                @Param("entity") TestBEntity entity) throws Exception;

    @Insert("Insert into ${tablename} " +
            "(ID," +
            "SHARD_MAP," +
            "C_VALUE," +
            "C_B_REF," +
            "C_NEW_VALUE," +
            "C_EXECUTE_TIME" +
            ") " +
            "values" +
            "(#{entity.id}," +
            "#{entity.shardMap}," +
            "#{entity.value}," +
            "#{entity.b}," +
            "#{entity.newValue}," +
            "#{entity.executeTime}" +
            ")"
    )
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    void insertC(@Param("tablename") String tableName,
                @Param("entity") TestCEntity entity) throws Exception;

    @Select("select * from ${tablename} " +
            "where id = ${idEntity}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "shardMap", column = "SHARD_MAP"),
            @Result(property = "value", column = "C_VALUE"),
            @Result(property = "newValue", column = "C_NEW_VALUE"),
            @Result(property = "executeTime", column = "C_EXECUTE_TIME")
    })
    TestBEntity findById(@Param("tablename") String tableName,
                               @Param("idEntity") Long id) throws Exception;

    @Select("select * from ${tablename} " +
            "where C_B_REF = ${idEntity}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "shardMap", column = "SHARD_MAP"),
            @Result(property = "value", column = "C_VALUE"),
            @Result(property = "b", column = "C_B_REF"),
            @Result(property = "newValue", column = "C_NEW_VALUE"),
            @Result(property = "executeTime", column = "C_EXECUTE_TIME")
    })
    List<TestCEntity> findAllC(@Param("tablename") String tableName,
                                      @Param("idEntity") Long id) throws Exception;

    @Select("select * from ${tablename} " +
            "where C_VALUE = ${value}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "shardMap", column = "SHARD_MAP"),
            @Result(property = "value", column = "C_VALUE"),
            @Result(property = "newValue", column = "C_NEW_VALUE"),
            @Result(property = "executeTime", column = "C_EXECUTE_TIME")
    })
    List<TestBEntity> findAllB(@Param("tablename") String tableName,
                         @Param("value") String value) throws Exception;

}
