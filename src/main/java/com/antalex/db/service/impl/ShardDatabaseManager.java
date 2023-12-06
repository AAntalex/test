package com.antalex.db.service.impl;

import com.antalex.db.model.Cluster;
import com.antalex.db.model.Shard;
import com.antalex.db.service.DataBaseManager;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

@Service
public class ShardDatabaseManager implements DataBaseManager {
    private final Environment env;

    private Cluster defaultCluster;
    private Map<String, Cluster> clusters = new HashMap<>();
    private Map<Short, Cluster> clusterIds = new HashMap<>();

    private static final int MAX_SHARDS = 63;
    private static final int MAX_CLUSTERS = 99;

    private static final String CLUSTER_NAME = "dbConfig.clusters[%d].name";
    private static final String CLUSTER_ID = "dbConfig.clusters[%d].id";
    private static final String CLUSTER_DEFAULT = "dbConfig.clusters[%d].default";
    private static final String SHARD_MAIN = "dbConfig.clusters[%d].shards[%d].main";
    private static final String SHARD_ID = "dbConfig.clusters[%d].shards[%d].id";
    private static final String SHARD_DB_DRIVER = "dbConfig.clusters[%d].shards[%d].database.driver";
    private static final String SHARD_DB_URL = "dbConfig.clusters[%d].shards[%d].database.url";
    private static final String SHARD_DB_USER = "dbConfig.clusters[%d].shards[%d].database.user";
    private static final String SHARD_DB_PASS = "dbConfig.clusters[%d].shards[%d].database.pass";

    ShardDatabaseManager(Environment env) {
        this.env = env;
        getProperties();
    }

    private void getProperties() {
        int clusterCount = 0;
        String clusterName = env.getProperty(String.format(CLUSTER_NAME, clusterCount));
        while (Objects.nonNull(clusterName)) {
            Cluster cluster = new Cluster();
            cluster.setName(clusterName);
            if (Objects.isNull(defaultCluster) ||
                    Boolean.valueOf(env.getProperty(String.format(CLUSTER_DEFAULT, clusterCount))))
            {
                defaultCluster = cluster;
            }
            cluster.setId(
                    Optional.ofNullable(env.getProperty(String.format(CLUSTER_ID, clusterCount)))
                            .map(Short::valueOf)
                            .orElse(null)
            );
            this.addCluster(cluster.getId(), cluster);

            int shardCount = 0;
            String shardUrl = env.getProperty(String.format(SHARD_DB_URL, clusterCount, shardCount));
            while (Objects.nonNull(shardUrl)) {
                Shard shard = new Shard();
                DriverManagerDataSource dataSource = new DriverManagerDataSource();
                dataSource.setUrl(shardUrl);
                dataSource.setDriverClassName(
                        env.getProperty(String.format(SHARD_DB_DRIVER, clusterCount, shardCount))
                );
                dataSource.setUsername(
                        env.getProperty(String.format(SHARD_DB_USER, clusterCount, shardCount))
                );
                dataSource.setPassword(
                        env.getProperty(String.format(SHARD_DB_PASS, clusterCount, shardCount))
                );
                shard.setDataSource(dataSource);
                shard.setId(
                        Optional.ofNullable(env.getProperty(String.format(SHARD_ID, clusterCount, shardCount)))
                                .map(Short::valueOf)
                                .orElse(null)
                );
                this.addShardToCluster(shard, cluster);
                if (Objects.isNull(cluster.getMainShard()) ||
                        Boolean.valueOf(env.getProperty(String.format(SHARD_MAIN, clusterCount, shardCount))))
                {
                    cluster.setMainShard(shard);
                }
                cluster.getShards().add(shard);

                shardCount++;
                shardUrl = env.getProperty(String.format(SHARD_DB_URL, clusterCount, shardCount));
            }
            if (shardCount > MAX_SHARDS) {
                throw new IllegalArgumentException(
                        "The number of shards in cluster cannot be more than " + MAX_SHARDS
                );
            }
            cluster.setShardSequence(new SimpleSequenceGenerator(1L, (long) MAX_SHARDS));
            this.addCluster(clusterName, cluster);

            clusterCount++;
            clusterName = env.getProperty(String.format(CLUSTER_NAME, clusterCount));
        }
        if (clusterCount == 0) {
            throw new IllegalArgumentException("Property 'dbConfig.clusters' must not be empty");
        }
        if (clusterCount > MAX_CLUSTERS) {
            throw new IllegalArgumentException("The number of clusters cannot be more than " + MAX_CLUSTERS);
        }
    }

    private void addCluster(String name, Cluster cluster) {
        if (clusters.containsKey(name)) {
            throw new IllegalArgumentException(
                    String.format("The cluster with name %s already exists", name)
            );
        } else {
            clusters.put(name, cluster);
        }
    }

    private void addCluster(Short id, Cluster cluster) {
        if (Objects.isNull(id)) {
            return;
        }
        if (clusterIds.containsKey(id)) {
            throw new IllegalArgumentException(
                    String.format("The cluster with ID %d already exists", id)
            );
        } else {
            clusterIds.put(id, cluster);
        }
    }

    private void addShardToCluster(Shard shard, Cluster cluster) {
        if (Objects.isNull(shard.getId())) {
            return;
        }
        if (cluster.getShardMap().containsKey(shard.getId())) {
            throw new IllegalArgumentException(
                    String.format("The shard with ID %d already exists in cluster %s", shard.getId(), cluster.getName())
            );
        } else {
            cluster.getShardMap().put(shard.getId(), shard);
        }
    }

    private Connection getConnection(Shard shard) throws SQLException {
        if (Objects.nonNull(shard)) {
            return shard.getDataSource().getConnection();
        }
        return null;
    }

    public Connection getConnection(String clusterName) throws SQLException {
        return getConnection(
                Optional.ofNullable(clusterName)
                        .map(clusters::get)
                        .map(Cluster::getMainShard)
                        .orElse(null)
        );
    }

    public Connection getConnection(Short clusterId, Short shardId) throws SQLException {
        return getConnection(
                Optional.ofNullable(clusterId)
                        .map(clusterIds::get)
                        .map(Cluster::getShards)
                        .map(it -> it.get(shardId))
                        .orElse(null)
        );
    }

    @Override
    public Connection getConnection() throws SQLException {
        return getConnection(defaultCluster.getMainShard());
    }

    @Override
    public DataSource getDataSource() {
        return defaultCluster.getMainShard().getDataSource();
    }
}
