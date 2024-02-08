package com.antalex.db.service;

import com.antalex.db.model.Cluster;
import com.antalex.db.model.Shard;
import com.antalex.db.model.StorageAttributes;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public interface ShardDataBaseManager {
    Connection getConnection() throws SQLException;
    DataSource getDataSource();
    Cluster getCluster(Short id);
    Cluster getCluster(String clusterName);
    Cluster getDefaultCluster();
    Shard getShard(Cluster cluster, Short id);
    Long generateId(StorageAttributes storageAttributes);
    Connection getConnection(Short clusterId, Short shardId) throws SQLException;
    StorageAttributes getStorageAttributes(Long id, Long shardValue);
    long sequenceNextVal(String sequenceName, Shard shard);
    long sequenceNextVal(String sequenceName, Cluster cluster);
    long sequenceNextVal(String sequenceName);
    long sequenceNextVal();
}
