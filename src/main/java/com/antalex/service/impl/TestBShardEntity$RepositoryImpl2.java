package com.antalex.service.impl;

import com.antalex.db.model.Cluster;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.ShardEntityManager;
import com.antalex.db.service.ShardEntityRepository;
import com.antalex.domain.persistence.entity.shard.TestBShardEntity;
import com.antalex.domain.persistence.entity.shard.TestBShardEntityInterceptor$;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.sql.Connection;
import java.sql.PreparedStatement;

@Component
public class TestBShardEntity$RepositoryImpl2 {
    private static final ShardType SHARD_TYPE = ShardType.SHARDABLE;
    private static final String INS_QUERY = "INSERT INTO $$$.TEST_B (SHARD_VALUE,C_VALUE,C_A_REF,C_NEW_VALUE,ID) VALUES (?,?,?,?,?)";
    private static final String UPD_QUERY = "UPDATE $$$.TEST_B SET SHARD_VALUE=?,C_VALUE=?,C_A_REF=?,C_NEW_VALUE=? WHERE ID=?";

    @Autowired
    private ShardEntityManager entityManager;
    private final Cluster cluster;

    @Autowired
    TestBShardEntity$RepositoryImpl2(ShardDataBaseManager dataBaseManager) {
       this.cluster = dataBaseManager.getCluster(String.valueOf("DEFAULT"));
    }

/*
    private void persist(TestBShardEntity entity) {
        entityManager
                .getQuery(entity, entity.getStorageAttributes().getStored() ? UPD_QUERY : INS_QUERY)
                .filtr(TestBShardEntity::getValue)
                .bind(entity.getStorageAttributes().getShardValue())
                .bind(entity.getValue())
                .bind(entity.getId())
                .bind(entity.getNewValue())
                .addBatch();

        if (!entityManager.getTransaction().isActive()) {

            entityManager.getTransaction().commit();
        }

        try {

            Connection connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            PreparedStatement preparedStatement = connection.prepareStatement(INS_QUERY);


            preparedStatement.setObject(1, entity.getId());
            preparedStatement.setObject(2, entity.getStorageAttributes().getShardValue());
            preparedStatement.setObject(3, entity.getValue());
            preparedStatement.setObject(4, entity.getNewValue());
            preparedStatement.addBatch();


            preparedStatement.executeBatch();


            connection.commit();

            connection.close();


        } catch (Exception err) {
            throw new RuntimeException(err);
        }

    }

*/
    public TestBShardEntity newEntity(Class<TestBShardEntity> clazz) {
       return new TestBShardEntityInterceptor$();
    }

    public TestBShardEntity save(TestBShardEntity entity) {
       entityManager.setStorage(entity, null, true);
       entityManager.generateId(entity, true);
       return entity;
   }

    public ShardType getShardType(TestBShardEntity entity) {
       return SHARD_TYPE;
    }

    public Cluster getCluster(TestBShardEntity entity) {
       return cluster;
    }

    public void setDependentStorage(TestBShardEntity entity) {
        entityManager.setStorage(entity.getA(), entity.getStorageAttributes());
        entityManager.setAllStorage(entity.getCList(), null);
    }

    public void generateDependentId(TestBShardEntity entity) {
        entityManager.generateId(entity.getA());
        entityManager.generateAllId(entity.getCList());
        entity.getCList().forEach(it -> it.setB(entity.getId()));
    }
}
