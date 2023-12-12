package com.antalex.db.service.impl;

import com.antalex.db.model.Cluster;
import com.antalex.db.model.Shard;
import com.antalex.db.service.DataBaseManager;
import com.antalex.db.service.LiquibaseManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

@Slf4j
@Service
public class ShardDatabaseManager implements DataBaseManager {
    private static final int MAX_SHARDS = 64;
    private static final int MAX_CLUSTERS = 100;

    private static final String INIT_CHANGE_LOG = "db/core/db.changelog-init.yaml";

    private static final String CLUSTER_NAME = "dbConfig.clusters[%d].name";
    private static final String CLUSTER_ID = "dbConfig.clusters[%d].id";
    private static final String CLUSTER_DEFAULT = "dbConfig.clusters[%d].default";
    private static final String SHARD_MAIN = "dbConfig.clusters[%d].shards[%d].main";
    private static final String SHARD_ID = "dbConfig.clusters[%d].shards[%d].id";
    private static final String SHARD_DB_DRIVER = "dbConfig.clusters[%d].shards[%d].database.driver";
    private static final String SHARD_DB_URL = "dbConfig.clusters[%d].shards[%d].database.url";
    private static final String SHARD_DB_USER = "dbConfig.clusters[%d].shards[%d].database.user";
    private static final String SHARD_DB_PASS = "dbConfig.clusters[%d].shards[%d].database.pass";

    private final Environment env;
    private final LiquibaseManager liquibaseManager;

    private Cluster defaultCluster;
    private Map<String, Cluster> clusters = new HashMap<>();
    private Map<Short, Cluster> clusterIds = new HashMap<>();
    private Map<Shard, Pair<Thread, String>> liquibaseRuns = new HashMap<>();

    ShardDatabaseManager(
            Environment env,
            LiquibaseManager liquibaseManager)
    {
        this.env = env;
        this.liquibaseManager = liquibaseManager;

        getProperties();
        runInitLiquibase();
    }

    private void getProperties() {
        int clusterCount = 0;
        String clusterName = env.getProperty(String.format(CLUSTER_NAME, clusterCount));
        while (Objects.nonNull(clusterName)) {
            Cluster cluster = new Cluster();
            cluster.setName(clusterName);
            if (Objects.isNull(defaultCluster) ||
                    Boolean.valueOf(env.getProperty(String.format(CLUSTER_DEFAULT, clusterCount)))) {
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
                        Boolean.valueOf(env.getProperty(String.format(SHARD_MAIN, clusterCount, shardCount)))) {
                    cluster.setMainShard(shard);
                }
                cluster.getShards().add(shard);

                shardCount++;
                shardUrl = env.getProperty(String.format(SHARD_DB_URL, clusterCount, shardCount));
            }
            if (shardCount > MAX_SHARDS) {
                throw new IllegalArgumentException(
                        "Number of shards in cluster cannot be more than " + MAX_SHARDS
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
            throw new IllegalArgumentException("Number of clusters cannot be more than " + MAX_CLUSTERS);
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
        if (cluster.getId() > MAX_CLUSTERS-1) {
            throw new IllegalArgumentException("ID of cluster cannot be more than " + (MAX_CLUSTERS-1));
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
        if (shard.getId() > MAX_SHARDS-1) {
            throw new IllegalArgumentException("ID of Shard cannot be more than " + (MAX_SHARDS-1));
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

    private synchronized void beforeRunLiquibase(Shard shard, String changeLog) {
        if (liquibaseRuns.containsKey(shard)) {
            throw new RuntimeException(
                    String.format("Cannot run %s for shard %s because %s is still running", changeLog, shard, liquibaseRuns.get(shard).getValue())
            );
        } else {
            liquibaseRuns.put(shard, ImmutablePair.of(Thread.currentThread(), changeLog));
        }
    }

    private synchronized void afterRunLiquibase(Shard shard, String changeLog) {
        liquibaseRuns.remove(shard);
    }

    private synchronized void waitRunLiquibase() {
        liquibaseRuns.entrySet()
                .stream()
                .findAny()
                .map(Map.Entry::getValue)
                .map(Pair::getKey)
                .ifPresent(it -> {
                    try {
                        log.info(String.format("Waiting thread %s ...", it));
                        it.join();
                    } catch (InterruptedException err) {
                        log.info(String.format("Thread %s is Interrupted", it));
                    }
                });
    }

    private void runLiquibase(Shard shard, String changeLog) {

        System.out.println(String.format("AAA START! %s для %s", changeLog, shard));

        new Thread(() -> {
            try {
                this.beforeRunLiquibase(shard, changeLog);
                liquibaseManager.run(getConnection(shard), changeLog);
                this.afterRunLiquibase(shard, changeLog);
            } catch (Exception err) {
                throw new RuntimeException(err);
            }
        }).start();
    }

    private void runLiquibase(Cluster cluster, String changeLog) {
        cluster
                .getShards()
                .forEach(shard -> runLiquibase(shard, changeLog));
    }

    private void runLiquibase(String changeLog) {
        clusters.entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .forEach(cluster -> runLiquibase(cluster, changeLog));
    }

    private void runInitLiquibase() {
        runLiquibase(INIT_CHANGE_LOG);
        waitRunLiquibase();
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
