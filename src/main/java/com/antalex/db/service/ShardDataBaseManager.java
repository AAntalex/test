package com.antalex.db.service;

import com.antalex.db.model.Cluster;
import com.antalex.db.model.Shard;
import com.antalex.db.model.StorageAttributes;
import com.antalex.db.model.enums.ShardType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public interface ShardDataBaseManager {
    Connection getConnection() throws SQLException;
    DataSource getDataSource();
    Cluster getCluster(Short id);
    Cluster getCluster(String clusterName);
    Shard getShard(Cluster cluster, Short id);
    Long generateId(StorageAttributes storageAttributes);
    Connection getConnection(Short clusterId, Short shardId) throws SQLException;
    StorageAttributes getStorageAttributes(Short id, Long shardValue);
}
