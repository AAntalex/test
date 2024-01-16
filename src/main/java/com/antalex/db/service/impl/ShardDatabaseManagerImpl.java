package com.antalex.db.service.impl;

import com.antalex.db.model.Cluster;
import com.antalex.db.model.DataBaseInfo;
import com.antalex.db.model.Shard;
import com.antalex.db.model.StorageAttributes;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.LiquibaseManager;
import com.antalex.db.service.SequenceGenerator;
import com.antalex.db.utils.ShardUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Slf4j
@Service
public class ShardDatabaseManagerImpl implements ShardDataBaseManager {
    private static final String INIT_CHANGE_LOG = "db/core/db.changelog-init.yaml";
    private static final String CLASSPATH = "classpath:";
    private static final String DEFAULT_CHANGE_LOG_PATH = "classpath:db/changelog";
    private static final String DEFAULT_CHANGE_LOG_NAME = "db.changelog-master.yaml";
    private static final String CLUSTERS_PATH = "clusters";
    private static final String SHARDS_PATH = "shards";
    private static final String MAIN_SEQUENCE = "SEQ_ID";

    private static final String CHANGE_LOG_PATH = "dbConfig.liquibase.change-log-src";
    private static final String CHANGE_LOG_NAME = "dbConfig.liquibase.change-log-name";
    private static final String CLUSTER_NAME = "dbConfig.clusters[%d].name";
    private static final String CLUSTER_ID = "dbConfig.clusters[%d].id";
    private static final String CLUSTER_DEFAULT = "dbConfig.clusters[%d].default";
    private static final String SHARD_MAIN = "dbConfig.clusters[%d].shards[%d].main";
    private static final String SHARD_ID = "dbConfig.clusters[%d].shards[%d].id";
    private static final String SHARD_DB_DRIVER = "dbConfig.clusters[%d].shards[%d].database.driver";
    private static final String SHARD_DB_URL = "dbConfig.clusters[%d].shards[%d].database.url";
    private static final String SHARD_DB_USER = "dbConfig.clusters[%d].shards[%d].database.user";
    private static final String SHARD_DB_PASS = "dbConfig.clusters[%d].shards[%d].database.pass";
    private static final String SHARD_DB_OWNER = "dbConfig.clusters[%d].shards[%d].database.owner";


    private static final String SELECT_DB_INFO = "SELECT SHARD_ID,MAIN_SHARD,CLUSTER_ID,CLUSTER_NAME,DEFAULT_CLUSTER" +
            " FROM $$$.APP_DATABASE";
    private static final String INS_DB_INFO = "INSERT INTO $$$.APP_DATABASE " +
            "(SHARD_ID,MAIN_SHARD,CLUSTER_ID,CLUSTER_NAME,DEFAULT_CLUSTER) " +
            " VALUES (?, ?, ?, ?, ?)";

    private final Environment env;
    private final ResourceLoader resourceLoader;
    private final LiquibaseManager liquibaseManager;

    private Cluster defaultCluster;
    private Map<String, Cluster> clusters = new HashMap<>();
    private Map<Short, Cluster> clusterIds = new HashMap<>();
    private Map<Cluster, SequenceGenerator> shardSequences = new HashMap<>();
    private Map<Cluster, SequenceGenerator> sequences = new HashMap<>();
    private List<ImmutablePair<Cluster, Shard>> newShards = new ArrayList<>();

    private String changLogPath;
    private String changLogName;

    ShardDatabaseManagerImpl(
            Environment env,
            ResourceLoader resourceLoader)
    {
        this.env = env;
        this.liquibaseManager = new LiquibaseManagerImpl();
        this.resourceLoader = resourceLoader;

        getProperties();
        runInitLiquibase();
        processDataBaseInfo();
        runLiquibase();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return getConnection(defaultCluster.getMainShard());
    }

    @Override
    public DataSource getDataSource() {
        return defaultCluster.getMainShard().getDataSource();
    }

    @Override
    public Cluster getCluster(Short id) {
        Assert.notNull(id, "Не указан идентификатор кластера");
        Cluster cluster = clusterIds.get(id);
        Assert.notNull(cluster, String.format("Отсутсвует кластер с идентификатором '%d'", id));
        return cluster;
    }

    @Override
    public Cluster getCluster(String clusterName) {
        Assert.notNull(clusterName, "Не указано наименование кластера");
        if (ShardUtils.DEFAULT_CLUSTER_NAME.equals(clusterName)) {
            return defaultCluster;
        } else {
            Cluster cluster = clusters.get(clusterName);
            Assert.notNull(cluster, String.format("Отсутсвует кластер с наименованием '%s'", clusterName));
            return cluster;
        }
    }

    @Override
    public Shard getShard(Cluster cluster, Short id) {
        Assert.notNull(cluster, "Не указан кластер");
        Assert.notNull(id, "Не указан идентификатор шарды");
        Shard shard = cluster.getShardMap().get(id);
        Assert.notNull(
                cluster,
                String.format("Отсутсвует шарда с идентификатором '%d' в кластере '%s'", id, cluster.getName())
        );
        return shard;
    }

    @Override
    public Long generateId(StorageAttributes storageAttributes) {
        Assert.notNull(storageAttributes, "Не определены аттрибуты хранения");
        Assert.notNull(
                storageAttributes.getCluster(),
                "Не верно определены аттрибуты хранения. Не определен кластер"
        );
        if (Objects.isNull(storageAttributes.getShard())) {
            storageAttributes.setShard(
                    getNextShard(storageAttributes.getCluster())
            );
        }
        SequenceGenerator sequenceGenerator = sequences.get(storageAttributes.getCluster());
        if (!sequences.containsKey(storageAttributes.getCluster())) {
            sequenceGenerator = new ApplicationSequenceGenerator(
                    MAIN_SEQUENCE,
                    storageAttributes.getCluster().getMainShard()
            );
            sequences.put(storageAttributes.getCluster(), sequenceGenerator);
        }
        return (
                sequenceGenerator.nextValue() * ShardUtils.MAX_CLUSTERS + storageAttributes.getCluster().getId()
        ) * ShardUtils.MAX_SHARDS + storageAttributes.getShard().getId();
    }

    @Override
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
    public StorageAttributes getStorageAttributes(Short id, Long shardValue) {
        Assert.notNull(id, "Не указан идентификатор сущности");
        StorageAttributes storageAttributes = new StorageAttributes();
        storageAttributes.setStored(true);
        storageAttributes.setCluster(getCluster((short) (id / ShardUtils.MAX_SHARDS % ShardUtils.MAX_CLUSTERS)));
        storageAttributes.setShard(getShard(storageAttributes.getCluster(), (short) (id % ShardUtils.MAX_SHARDS)));
        storageAttributes.setShardValue(shardValue);
        return storageAttributes;
    }

    private Shard getNextShard(Cluster cluster) {
        return cluster.getShards().get((int) shardSequences.get(cluster).nextValue());
    }

    private void processDataBaseInfo() {
        getDataBaseInfo();
        checkDataBaseInfo();
        saveDataBaseInfo();
    }

    private void checkShardID(Cluster cluster, Shard shard, short shardId) {
        if (Objects.isNull(shard.getId())) {
            shard.setId(shardId);
            this.addShardToCluster(cluster, shard);
        } else {
            Assert.isTrue(
                    shard.getId().equals(shardId),
                    String.format(
                            "Идентификатор шарды в настройках 'dbConfig.clusters.shards.id' = '%d' " +
                                    "кластера '%s' " +
                                    "не соответсвует идентификатору в БД = '%d'.",
                            shard.getId(), cluster.getName(), shardId
                    )
            );
        }
    }

    private void checkMainShard(Cluster cluster, Shard shard, boolean mainShard) {
        Assert.isTrue(
                shard.getId().equals(cluster.getMainShard().getId()) == mainShard,
                String.format(
                        "Шарда с ID = '%d'%s должна быть основной в Кластере '%s'" ,
                        shard.getId(),
                        mainShard ? "" : " не",
                        cluster.getName()
                )
        );
    }

    private void checkClusterID(Cluster cluster, short clusterId) {
        if (Objects.isNull(cluster.getId())) {
            cluster.setId(clusterId);
            this.addCluster(cluster.getId(), cluster);
        } else {
            Assert.isTrue(
                    cluster.getId().equals(clusterId),
                    String.format(
                            "Идентификатор кластера '%s' в настройках 'dbConfig.clusters.id' = '%d' " +
                                    "не соответсвует идентификатору в БД = '%d'.",
                            cluster.getName(), cluster.getId(), clusterId
                    )
            );
        }
    }

    private void checkClusterName(Cluster cluster, String clusterName) {
        Assert.isTrue(
                cluster.getName().equals(clusterName),
                String.format(
                        "Наименование кластера '%s' в настройках 'dbConfig.clusters.name' = '%s' " +
                                "не соответсвует наименованию в БД = '%s'.",
                        cluster.getName(), cluster.getName(), clusterName
                )
        );
    }

    private void checkClusterDefault(Cluster cluster, boolean clusterDefault) {
        Assert.isTrue(
                cluster.getName().equals(defaultCluster.getName()) == clusterDefault,
                String.format(
                        "Кластер '%s'%s должен быть основным" ,
                        cluster.getName(),
                        clusterDefault ? "" : " не"
                )
        );
    }

    private short getShardId(Cluster cluster) {
        for (short i = 0; i < ShardUtils.MAX_SHARDS; i++) {
            if (!cluster.getShardMap().containsKey(i)) {
                return i;
            }
        }
        throw new IllegalStateException(
                String.format("Отсутсвует свободный идентификатор для шарды в кластере %s", cluster.getName())
        );
    }

    private short getClusterId() {
        for (short i = 0; i < ShardUtils.MAX_SHARDS; i++) {
            if (!clusterIds.containsKey(i)) {
                return i;
            }
        }
        throw new IllegalStateException("Отсутсвует свободный идентификатор для кластера");
    }

    private void checkDataBaseInfo(Cluster cluster, Shard shard) {
        if (Objects.nonNull(shard.getDataBaseInfo())) {
            checkShardID(cluster, shard, shard.getDataBaseInfo().getShardId());
            checkMainShard(cluster, shard, shard.getDataBaseInfo().isMainShard());
            checkClusterID(cluster, shard.getDataBaseInfo().getClusterId());
            checkClusterName(cluster, shard.getDataBaseInfo().getClusterName());
            checkClusterDefault(cluster, shard.getDataBaseInfo().isDefaultCluster());
        } else {
            newShards.add(ImmutablePair.of(cluster, shard));
        }
    }

    private void checkDataBaseInfo(Cluster cluster) {
        for (Shard shard : cluster.getShards()) {
            checkDataBaseInfo(cluster, shard);
        }
    }

    private void checkDataBaseInfo() {
        clusters.entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .forEach(this::checkDataBaseInfo);
    }

    private void getDataBaseInfo(Shard shard) {
        try {
            Connection connection = shard.getDataSource().getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    ShardUtils.transformSQL(SELECT_DB_INFO, shard)
            );
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                shard.setDataBaseInfo(
                        DataBaseInfo
                                .builder()
                                .shardId(resultSet.getShort(1))
                                .mainShard(resultSet.getBoolean(2))
                                .clusterId(resultSet.getShort(3))
                                .clusterName(resultSet.getString(4))
                                .defaultCluster(resultSet.getBoolean(5))
                                .build()
                );
            }
            connection.close();
        } catch (SQLException err) {
            throw new RuntimeException(err);
        }
    }

    private void getDataBaseInfo(Cluster cluster) {
        cluster
                .getShards()
                .forEach(this::getDataBaseInfo);
    }

    private void getDataBaseInfo() {
        clusters.entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .forEach(this::getDataBaseInfo);
    }

    private void saveDataBaseInfo(Cluster cluster, Shard shard) {
        if (Objects.isNull(shard.getDataBaseInfo())) {
            if (Objects.isNull(shard.getId())) {
                shard.setId(getShardId(cluster));
                this.addShardToCluster(cluster, shard);
            }
            if (Objects.isNull(cluster.getId())) {
                cluster.setId(getClusterId());
                this.addCluster(cluster.getId(), cluster);
            }
            shard.setDataBaseInfo(
                    DataBaseInfo
                            .builder()
                            .shardId(shard.getId())
                            .mainShard(shard.getId().equals(cluster.getMainShard().getId()))
                            .clusterId(cluster.getId())
                            .clusterName(cluster.getName())
                            .defaultCluster(cluster.getName().equals(defaultCluster.getName()))
                            .build()
            );
            try {
                Connection connection = shard.getDataSource().getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(
                        ShardUtils.transformSQL(INS_DB_INFO, shard)
                );

                preparedStatement.setShort(1, shard.getDataBaseInfo().getShardId());
                preparedStatement.setBoolean(2, shard.getDataBaseInfo().isMainShard());
                preparedStatement.setShort(3, shard.getDataBaseInfo().getClusterId());
                preparedStatement.setString(4, shard.getDataBaseInfo().getClusterName());
                preparedStatement.setBoolean(5, shard.getDataBaseInfo().isDefaultCluster());

                preparedStatement.executeUpdate();
                connection.close();
            } catch (SQLException err) {
                throw new RuntimeException(err);
            }
        }
    }

    private void saveDataBaseInfo() {
        newShards
                .forEach(it -> saveDataBaseInfo(it.getLeft(), it.getRight()));
    }

    private void getProperties() {
        this.changLogPath = env.getProperty(CHANGE_LOG_PATH, DEFAULT_CHANGE_LOG_PATH);
        this.changLogName = env.getProperty(CHANGE_LOG_NAME, DEFAULT_CHANGE_LOG_NAME);

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
                shard.setOwner(env.getProperty(String.format(SHARD_DB_OWNER, clusterCount, shardCount)));
                if (Objects.isNull(shard.getOwner())) {
                    shard.setOwner(dataSource.getUsername());
                }
                shard.setId(
                        Optional.ofNullable(env.getProperty(String.format(SHARD_ID, clusterCount, shardCount)))
                                .map(Short::valueOf)
                                .orElse(null)
                );
                this.addShardToCluster(cluster, shard);
                if (Objects.isNull(cluster.getMainShard()) ||
                        Boolean.valueOf(env.getProperty(String.format(SHARD_MAIN, clusterCount, shardCount)))) {
                    cluster.setMainShard(shard);
                }
                cluster.getShards().add(shard);

                shardCount++;
                shardUrl = env.getProperty(String.format(SHARD_DB_URL, clusterCount, shardCount));
            }
            Assert.isTrue(shardCount > 0, "Property 'dbConfig.clusters.shards' must not be empty");
            Assert.isTrue(
                    shardCount <= ShardUtils.MAX_SHARDS,
                    "Number of shards in cluster cannot be more than " + ShardUtils.MAX_SHARDS
            );
            shardSequences.put(cluster, new SimpleSequenceGenerator(1L, (long) cluster.getShards().size()));
            this.addCluster(clusterName, cluster);

            clusterCount++;
            clusterName = env.getProperty(String.format(CLUSTER_NAME, clusterCount));
        }
        Assert.isTrue(clusterCount > 0, "Property 'dbConfig.clusters' must not be empty");
        Assert.isTrue(
                clusterCount <= ShardUtils.MAX_CLUSTERS,
                "Number of clusters cannot be more than " + ShardUtils.MAX_CLUSTERS
        );
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

    private synchronized void addCluster(Short id, Cluster cluster) {
        if (Objects.isNull(id)) {
            return;
        }
        if (cluster.getId() > ShardUtils.MAX_CLUSTERS-1) {
            throw new IllegalArgumentException("ID of cluster cannot be more than " + (ShardUtils.MAX_CLUSTERS-1));
        }
        if (clusterIds.containsKey(id)) {
            throw new IllegalArgumentException(
                    String.format("The cluster with ID %d already exists", id)
            );
        } else {
            clusterIds.put(id, cluster);
        }
    }

    private synchronized void addShardToCluster(Cluster cluster, Shard shard) {
        if (Objects.isNull(shard.getId())) {
            return;
        }
        if (shard.getId() > ShardUtils.MAX_SHARDS-1) {
            throw new IllegalArgumentException("ID of Shard cannot be more than " + (ShardUtils.MAX_SHARDS-1));
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

    private Connection getConnection(String clusterName) throws SQLException {
        return getConnection(
                Optional.ofNullable(clusterName)
                        .map(clusters::get)
                        .map(Cluster::getMainShard)
                        .orElse(null)
        );
    }

    private void runLiquibase(Shard shard, String changeLog) {
        try {
            liquibaseManager.runThread(
                    getConnection(shard),
                    shard,
                    changeLog.startsWith(CLASSPATH) ? changeLog.substring(CLASSPATH.length()) : changeLog,
                    shard.getOwner()
            );
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
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
        liquibaseManager.waitAll();
    }

    private void runLiquibaseFromPath(String path, Shard shard) {
        Optional.of(path + File.separatorChar + this.changLogName)
                .filter(src -> resourceLoader.getResource(src).exists())
                .ifPresent(changeLog -> runLiquibase(shard, changeLog));
    }

    private void runLiquibaseFromPath(String path, Cluster cluster) {
        Optional.of(path + File.separatorChar + this.changLogName)
                .filter(src -> resourceLoader.getResource(src).exists())
                .ifPresent(changeLog -> runLiquibase(cluster, changeLog));
    }

    private void runLiquibaseFromPath(String path) {
        Optional.of(path + File.separatorChar + this.changLogName)
                .filter(src -> resourceLoader.getResource(src).exists())
                .ifPresent(this::runLiquibase);
    }

    private void runLiquibase() {
        Optional.ofNullable(this.changLogPath)
                .filter(src -> resourceLoader.getResource(src).exists())
                .ifPresent(path -> {
                    Optional.of(path + File.separatorChar + CLUSTERS_PATH)
                            .filter(src -> resourceLoader.getResource(src).exists())
                            .ifPresent(clustersPath -> {
                                runLiquibaseFromPath(clustersPath);
                                clusters.forEach((clusterName, cluster) -> {
                                    Optional.of(clustersPath + File.separatorChar + clusterName)
                                            .filter(src -> resourceLoader.getResource(src).exists())
                                            .ifPresent(clusterPath -> {
                                                runLiquibaseFromPath(clusterPath, cluster);
                                                Optional.of(clusterPath + File.separatorChar + SHARDS_PATH)
                                                        .filter(src -> resourceLoader.getResource(src).exists())
                                                        .ifPresent(shardsPath -> {
                                                            runLiquibaseFromPath(shardsPath, cluster);
                                                            cluster
                                                                    .getShards()
                                                                    .forEach(shard ->
                                                                            runLiquibaseFromPath(
                                                                                    shardsPath +
                                                                                            File.separatorChar +
                                                                                            shard.getId()
                                                                                    , shard
                                                                            )
                                                                    );
                                                        });
                                            });
                                });
                            });
                    runLiquibaseFromPath(path, defaultCluster.getMainShard());
                });

        liquibaseManager.waitAll();
    }
}
