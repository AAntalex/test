package com.antalex.db.service;

import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.Cluster;
import com.antalex.db.model.Shard;
import com.antalex.db.service.api.TransactionalTask;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.stream.Stream;

public interface ShardDataBaseManager {
    Connection getConnection() throws SQLException;
    DataSource getDataSource();
    Cluster getCluster(Short id);
    Cluster getCluster(String clusterName);
    Cluster getDefaultCluster();
    Shard getShard(Cluster cluster, Short id);
    Stream<Shard> getAllShards(ShardInstance entity);
    Stream<Shard> getNewShards(ShardInstance entity);
    void generateId(ShardInstance entity);
    Connection getConnection(Short clusterId, Short shardId) throws SQLException;
    StorageAttributes getStorageAttributes(Long id, Long shardValue);
    long sequenceNextVal(String sequenceName, Shard shard);
    long sequenceNextVal(String sequenceName, Cluster cluster);
    long sequenceNextVal(String sequenceName);
    long sequenceNextVal();
    TransactionalTask getTransactionalTask(Shard shard);
    Boolean isEnabled(Shard shard);
}
