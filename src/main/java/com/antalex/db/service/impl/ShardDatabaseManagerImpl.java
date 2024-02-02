package com.antalex.db.service.impl;

import com.antalex.db.api.SQLRunnable;
import com.antalex.db.config.*;
import com.antalex.db.entity.abstraction.ShardInstance;
import com.antalex.db.model.*;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.LiquibaseManager;
import com.antalex.db.service.SequenceGenerator;
import com.antalex.db.service.ShardEntityManager;
import com.antalex.db.utils.ShardUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private static final int DEFAULT_TIME_OUT_REFRESH_DB_INFO = 10;

    private static final String SQL_ERROR_TEXT = "Ошибки при выполнении SQL запроса: ";
    private static final String SQL_ERROR_COMMIT_TEXT = "Ошибки при подтверждении транзакции: ";
    private static final String SQL_ERROR_ROLLBACK_TEXT = "Ошибки при откате транзакции: ";
    private static final String SQL_ERROR_PREFIX = "   : ";

    private static final String SELECT_DB_INFO = "SELECT SHARD_ID,MAIN_SHARD,CLUSTER_ID,CLUSTER_NAME,DEFAULT_CLUSTER" +
            ",SEGMENT_NAME,ACCESSIBLE FROM $$$.APP_DATABASE";
    private static final String INS_DB_INFO = "INSERT INTO $$$.APP_DATABASE " +
            "(SHARD_ID,MAIN_SHARD,CLUSTER_ID,CLUSTER_NAME,DEFAULT_CLUSTER,ACCESSIBLE) " +
            " VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SELECT_DYNAMIC_DB_INFO = "SELECT SEGMENT_NAME,ACCESSIBLE FROM $$$.APP_DATABASE";

    private final ResourceLoader resourceLoader;
    private final LiquibaseManager liquibaseManager;
    private final ShardDataBaseConfig shardDataBaseConfig;

    private Cluster defaultCluster;
    private Map<String, Cluster> clusters = new HashMap<>();
    private Map<Short, Cluster> clusterIds = new HashMap<>();
    private Map<Cluster, SequenceGenerator> shardSequences = new HashMap<>();
    private Map<String, Map<Shard, SequenceGenerator>> sequences = new HashMap<>();
    private List<ImmutablePair<Cluster, Shard>> newShards = new ArrayList<>();

    private String changLogPath;
    private String changLogName;
    private String segment;
    private int timeOut;

    ShardDatabaseManagerImpl(
            ResourceLoader resourceLoader,
            ShardDataBaseConfig shardDataBaseConfig)
    {
        this.liquibaseManager = new LiquibaseManagerImpl();
        this.resourceLoader = resourceLoader;
        this.shardDataBaseConfig = shardDataBaseConfig;

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
    public Cluster getDefaultCluster() {
        return defaultCluster;
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
    public <T extends ShardInstance> Long generateId(T entity) {
        StorageAttributes storageAttributes = entity.getStorageAttributes();
        Assert.notNull(storageAttributes, "Не определены аттрибуты хранения");
        Assert.notNull(
                storageAttributes.getCluster(),
                "Не верно определены аттрибуты хранения. Не определен кластер"
        );
        if (Objects.isNull(storageAttributes.getShard())) {
            storageAttributes.setShard(
                    getNextShard(storageAttributes.getCluster())
            );
            storageAttributes.setShardValue(ShardUtils.getShardValue(storageAttributes.getShard().getId()));
        }
        return (
                sequenceNextVal(MAIN_SEQUENCE, storageAttributes.getCluster()) *
                        ShardUtils.MAX_CLUSTERS + storageAttributes.getCluster().getId() - 1
        ) * ShardUtils.MAX_SHARDS + storageAttributes.getShard().getId() - 1;
    }

    @Override
    public long sequenceNextVal(String sequenceName, Shard shard) {
        Map<Shard, SequenceGenerator> shardSequences = sequences.get(sequenceName);
        if (Objects.isNull(shardSequences)) {
            shardSequences = new HashMap<>();
            sequences.put(sequenceName, shardSequences);
        }
        SequenceGenerator sequenceGenerator = shardSequences.get(shard);
        if (Objects.isNull(sequenceGenerator)) {
            sequenceGenerator = new ApplicationSequenceGenerator(sequenceName, shard);
            shardSequences.put(shard, sequenceGenerator);
        }
        return sequenceGenerator.nextValue();
    }

    @Override
    public long sequenceNextVal(String sequenceName, Cluster cluster) {
        return sequenceNextVal(sequenceName, cluster.getMainShard());
    }

    @Override
    public long sequenceNextVal(String sequenceName) {
        return sequenceNextVal(sequenceName, getDefaultCluster());
    }

    @Override
    public long sequenceNextVal() {
        return sequenceNextVal(MAIN_SEQUENCE, getDefaultCluster());
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
        Cluster cluster = getCluster((short) (id / ShardUtils.MAX_SHARDS % ShardUtils.MAX_CLUSTERS + 1));
        Shard shard = getShard(cluster, (short) (id % ShardUtils.MAX_SHARDS + 1));
        return StorageAttributes.builder()
                .stored(true)
                .cluster(cluster)
                .shard(shard)
                .shardValue(shardValue)
                .build();
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
                cluster.getName().equals(getDefaultCluster().getName()) == clusterDefault,
                String.format(
                        "Кластер '%s'%s должен быть основным" ,
                        cluster.getName(),
                        clusterDefault ? "" : " не"
                )
        );
    }

    private short getShardId(Cluster cluster) {
        for (short i = 1; i <= ShardUtils.MAX_SHARDS; i++) {
            if (!cluster.getShardMap().containsKey(i)) {
                return i;
            }
        }
        throw new IllegalStateException(
                String.format("Отсутсвует свободный идентификатор для шарды в кластере %s", cluster.getName())
        );
    }

    private short getClusterId() {
        for (short i = 1; i <= ShardUtils.MAX_CLUSTERS; i++) {
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

    private void getDynamicDataBaseInfo(Shard shard) {
        if (System.currentTimeMillis() - shard.getDynamicDataBaseInfo().getLastTime() > this.timeOut) {
            DynamicDataBaseInfo dynamicDataBaseInfo = shard.getDynamicDataBaseInfo();
            dynamicDataBaseInfo.setLastTime(System.currentTimeMillis());
            try {
                Connection connection = getConnection(shard);
                PreparedStatement preparedStatement = connection.prepareStatement(
                        ShardUtils.transformSQL(SELECT_DYNAMIC_DB_INFO, shard)
                );
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    dynamicDataBaseInfo.setAvailable(true);
                    dynamicDataBaseInfo.setSegment(resultSet.getString(1));
                    dynamicDataBaseInfo.setAccessible(resultSet.getBoolean(2));
                }
                connection.close();
            } catch (SQLException err) {
                //dynamicDataBaseInfo.setAvailable(false);
                throw new RuntimeException(err);
            }
        }
    }

    private SQLRunInfo runSQLThread(Shard shard, SQLRunnable target, String description) {
        try {
            return runSQLThread(getConnection(shard), target, description);
        } catch (SQLException err) {
            throw new RuntimeException(err);
        }
    }

    private SQLRunInfo runSQLThread(Connection connection, SQLRunnable target, String description) {
        SQLRunInfo sqlRunInfo = new SQLRunInfo();
        Thread thread = new Thread(() -> {
            try {
                sqlRunInfo.setConnection(connection);
                target.run(connection);
            } catch (SQLException err) {
                sqlRunInfo.setError(err.getLocalizedMessage());
                throw new RuntimeException(err);
            }
        });
        sqlRunInfo.setThread(thread);
        sqlRunInfo.setDescription(description);
        thread.start();
        return sqlRunInfo;
    }

    private List<SQLRunInfo> getDataBaseInfo(Cluster cluster) {
        return cluster
                .getShards()
                .stream()
                .map(
                        shard ->
                                this.runSQLThread(
                                        shard,
                                        (connection) -> {
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
                                                shard.setDynamicDataBaseInfo(
                                                        DynamicDataBaseInfo
                                                                .builder()
                                                                .lastTime(System.currentTimeMillis())
                                                                .available(true)
                                                                .segment(resultSet.getString(6))
                                                                .accessible(resultSet.getBoolean(7))
                                                                .build()
                                                );
                                            }
                                           connection.close();
                                        },
                                        String.format("GET DataBase Info on shard '%s'", shard.getName())
                                )

                ).collect(Collectors.toList());
    }

    private void getDataBaseInfo() {
        SQLRun sqlRun = new SQLRun();
        clusters.entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .forEach(cluster -> sqlRun.getRuns().addAll(this.getDataBaseInfo(cluster)));

        procSQLRun(sqlRun, false);
    }

    private void procSQLRunError(SQLRunInfo sqlRunInfo, SQLRun sqlRun, String errorText) {
        if (Objects.nonNull(sqlRunInfo.getError())) {
            sqlRun.setHasError(true);
            sqlRun.setErrorMessage(
                    Optional.ofNullable(sqlRun.getErrorMessage())
                            .map(it -> it.concat(StringUtils.LF))
                            .orElse(errorText)
                            .concat(sqlRunInfo.getThread().getName())
                            .concat(SQL_ERROR_PREFIX)
                            .concat(sqlRunInfo.getError())
            );
        }
    }

    private void procSQLRunConnection(SQLRunInfo sqlRunInfo, SQLRun sqlRun) {
        try {
            if (Objects.nonNull(sqlRunInfo.getConnection()) && !sqlRunInfo.getConnection().isClosed()) {
                if (sqlRunInfo.getConnection().getAutoCommit()) {
                    sqlRunInfo.getConnection().close();
                } else {
                    sqlRun.setNeedCommit(true);
                }
            }
        } catch (SQLException err) {
            throw new RuntimeException(err);
        }
    }

    private SQLRun commitRuns(SQLRun sqlRun) {
        SQLRun sqlRunCommit = new SQLRun();
        sqlRunCommit.getRuns().addAll(
                sqlRun.getRuns()
                        .stream()
                        .map(run ->
                                runSQLThread(
                                        run.getConnection(),
                                        (connection) -> {
                                            if (!connection.isClosed() && !connection.getAutoCommit()) {
                                                if (sqlRun.getHasError()) {
                                                    connection.rollback();
                                                } else {
                                                    connection.commit();
                                                }
                                                connection.close();
                                            }},
                                        (sqlRun.getHasError() ? "ROLLBACK" : "COMMIT") +
                                                String.format(" for \"%s\"", run.getDescription())
                                )
                        ).collect(Collectors.toList())
        );
        return sqlRunCommit;
    }

    private void procSQLRun(SQLRun sqlRun, boolean isCommit) {
        sqlRun.getRuns().forEach(sqlRunInfo -> {
            try {
                log.debug(
                        String.format(
                                "Waiting %s for \"%s\"...",
                                sqlRunInfo.getThread().getName(),
                                sqlRunInfo.getDescription()
                        )
                );
                sqlRunInfo.getThread().join();
                procSQLRunError(
                        sqlRunInfo,
                        sqlRun,
                        isCommit
                                ? (sqlRun.getHasError() ? SQL_ERROR_ROLLBACK_TEXT : SQL_ERROR_COMMIT_TEXT)
                                : SQL_ERROR_TEXT
                );
                if (!isCommit && !sqlRun.getNeedCommit()) {
                    procSQLRunConnection(sqlRunInfo, sqlRun);
                }
            } catch (InterruptedException err) {
                throw new RuntimeException(err);
            }
        });
        if (!isCommit && sqlRun.getNeedCommit()) {
            procSQLRun(commitRuns(sqlRun), true);
        }
        if (sqlRun.getHasError()) {
            throw new RuntimeException(sqlRun.getErrorMessage());
        }
    }

    private SQLRunInfo saveDataBaseInfo(Cluster cluster, Shard shard) {
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
                        .defaultCluster(cluster.getName().equals(getDefaultCluster().getName()))
                        .build()
        );
        shard.setDynamicDataBaseInfo(
                DynamicDataBaseInfo
                        .builder()
                        .lastTime(System.currentTimeMillis())
                        .available(true)
                        .accessible(true)
                        .build()
        );

        return this.runSQLThread(
                shard,
                (connection) -> {
                    connection.setAutoCommit(false);
                    PreparedStatement preparedStatement = connection.prepareStatement(
                            ShardUtils.transformSQL(INS_DB_INFO, shard)
                    );

                    preparedStatement.setShort(1, shard.getDataBaseInfo().getShardId());
                    preparedStatement.setBoolean(2, shard.getDataBaseInfo().isMainShard());
                    preparedStatement.setShort(3, shard.getDataBaseInfo().getClusterId());
                    preparedStatement.setString(4, shard.getDataBaseInfo().getClusterName());
                    preparedStatement.setBoolean(5, shard.getDataBaseInfo().isDefaultCluster());
                    preparedStatement.setBoolean(6, true);

                    preparedStatement.executeUpdate();
                },
                String.format("SAVE DataBase Info on shard '%s'", shard.getName()));
    }

    private void saveDataBaseInfo() {
        SQLRun sqlRun = new SQLRun();
        newShards
                .stream()
                .filter(it -> Objects.isNull(it.getRight().getDataBaseInfo()))
                .forEach(it ->
                        sqlRun.getRuns().add(
                                saveDataBaseInfo(it.getLeft(), it.getRight())
                        )
                );
        procSQLRun(sqlRun, false);
    }

    private <T> void setHikariConfigValue(ShardDataBaseConfig shardDataBaseConfig,
                                          ClusterConfig clusterConfig,
                                          ShardConfig shardConfig,
                                          Function<HikariSettings, T> functionGet,
                                          Consumer<T> functionSet)
    {
        Optional.ofNullable(
                Optional.ofNullable(shardConfig.getHikari())
                        .map(functionGet)
                        .orElse(
                                Optional.ofNullable(clusterConfig.getHikari())
                                        .map(functionGet)
                                        .orElse(
                                                Optional.ofNullable(shardDataBaseConfig.getHikari())
                                                        .map(functionGet)
                                                        .orElse(null)
                                        )
                        )
        ).ifPresent(functionSet);
    }

    private static <T> void setDataBaseConfigValue(DataBaseConfig dataBaseConfig,
                                                   Function<DataBaseConfig, T> functionGet,
                                                   Consumer<T> functionSet)
    {
        Optional.ofNullable(dataBaseConfig).map(functionGet).ifPresent(functionSet);
    }

    private void setOptionalDataBaseConfig(HikariConfig config, DataBaseConfig dataBaseConfig) {
        setDataBaseConfigValue(dataBaseConfig, DataBaseConfig::getUrl, config::setJdbcUrl);
        setDataBaseConfigValue(dataBaseConfig, DataBaseConfig::getDriver, config::setDriverClassName);
        setDataBaseConfigValue(dataBaseConfig, DataBaseConfig::getClassName, config::setDataSourceClassName);
        setDataBaseConfigValue(dataBaseConfig, DataBaseConfig::getUser, config::setUsername);
        setDataBaseConfigValue(dataBaseConfig, DataBaseConfig::getPass, config::setPassword);
    }

    private void setOptionalHikariConfig(
            HikariConfig config,
            ShardDataBaseConfig shardDataBaseConfig,
            ClusterConfig clusterConfig,
            ShardConfig shardConfig)
    {
        setHikariConfigValue(shardDataBaseConfig, clusterConfig, shardConfig,
                HikariSettings::getMinimumIdle, config::setMinimumIdle
        );
        setHikariConfigValue(shardDataBaseConfig, clusterConfig, shardConfig,
                HikariSettings::getMaximumPoolSize, config::setMaximumPoolSize
        );
        setHikariConfigValue(shardDataBaseConfig, clusterConfig, shardConfig,
                HikariSettings::getIdleTimeout, config::setIdleTimeout
        );
        setHikariConfigValue(shardDataBaseConfig, clusterConfig, shardConfig,
                HikariSettings::getConnectionTimeout, config::setConnectionTimeout
        );
        setHikariConfigValue(shardDataBaseConfig, clusterConfig, shardConfig,
                HikariSettings::getMaxLifetime, config::setMaxLifetime
        );
        setHikariConfigValue(shardDataBaseConfig, clusterConfig, shardConfig,
                HikariSettings::getPoolName, config::setPoolName
        );
    }

    private HikariConfig getHikariConfig(
            ShardDataBaseConfig shardDataBaseConfig,
            ClusterConfig clusterConfig,
            ShardConfig shardConfig)
    {
        HikariConfig config = new HikariConfig();
        setOptionalDataBaseConfig(config, shardConfig.getDataBase());
        setOptionalHikariConfig(config, shardDataBaseConfig, clusterConfig, shardConfig);
        return config;
    }

    private void getProperties() {
        this.changLogPath = Optional
                .ofNullable(shardDataBaseConfig.getLiquibase())
                .map(LiquibaseConfig::getChangeLogSrc)
                .orElse(DEFAULT_CHANGE_LOG_PATH);
        this.changLogName = Optional
                .ofNullable(shardDataBaseConfig.getLiquibase())
                .map(LiquibaseConfig::getChangeLogName)
                .orElse(DEFAULT_CHANGE_LOG_NAME);
        this.segment = shardDataBaseConfig.getSegment();
        this.timeOut = Optional
                .ofNullable(shardDataBaseConfig.getTimeOut())
                .orElse(DEFAULT_TIME_OUT_REFRESH_DB_INFO) * 1000;

        Assert.notEmpty(
                shardDataBaseConfig.getClusters(),
                String.format("Property '%s.clusters' must not be empty", ShardDataBaseConfig.CONFIG_NAME)
        );
        Assert.isTrue(
                shardDataBaseConfig.getClusters().size() <= ShardUtils.MAX_CLUSTERS,
                "Number of clusters cannot be more than " + ShardUtils.MAX_CLUSTERS
        );
        shardDataBaseConfig.getClusters().forEach(clusterConfig->{
            Cluster cluster = new Cluster();
            cluster.setName(clusterConfig.getName());
            if (Objects.isNull(getDefaultCluster()) ||
                    Optional.ofNullable(clusterConfig.getDefaultCluster()).orElse(false))
            {
                defaultCluster = cluster;
            }
            cluster.setId(clusterConfig.getId());
            this.addCluster(cluster.getId(), cluster);

            Assert.notEmpty(
                    clusterConfig.getShards(),
                    String.format("Property '%s.clusters.shards' must not be empty", ShardDataBaseConfig.CONFIG_NAME)
            );
            Assert.isTrue(
                    clusterConfig.getShards().size() <= ShardUtils.MAX_SHARDS,
                    "Number of shards in cluster cannot be more than " + ShardUtils.MAX_SHARDS
            );

            clusterConfig.getShards().forEach(shardConfig-> {
                Shard shard = new Shard();
                HikariDataSource dataSource = new HikariDataSource(
                        getHikariConfig(shardDataBaseConfig, clusterConfig, shardConfig)
                );
                shard.setSequenceCacheSize(
                        Optional.ofNullable(shardConfig.getSequenceCacheSize())
                                .orElse(
                                        Optional.ofNullable(clusterConfig.getSequenceCacheSize())
                                                .orElse(
                                                        Optional.ofNullable(shardDataBaseConfig.getSequenceCacheSize())
                                                                .orElse(null)
                                                )
                                )
                );
                shard.setDataSource(dataSource);
                shard.setOwner(
                        Optional.ofNullable(shardConfig.getDataBase())
                                .map(DataBaseConfig::getOwner)
                                .orElse(dataSource.getUsername())
                );
                shard.setName(
                        String.format("%s: (%s)", cluster.getName(), shardConfig.getDataBase().getUrl())
                );
                shard.setId(shardConfig.getId());
                this.addShardToCluster(cluster, shard);

                if (Objects.isNull(cluster.getMainShard())
                        || Optional.ofNullable(shardConfig.getMain()).orElse(false))
                {
                    cluster.setMainShard(shard);
                }
                cluster.getShards().add(shard);
            });
            shardSequences.put(cluster, new SimpleSequenceGenerator(1L, (long) cluster.getShards().size()));
            this.addCluster(cluster.getName(), cluster);
        });
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
        if (cluster.getId() > ShardUtils.MAX_CLUSTERS) {
            throw new IllegalArgumentException("ID of cluster cannot be more than " + ShardUtils.MAX_CLUSTERS);
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
        if (shard.getId() < 1) {
            throw new IllegalArgumentException("ID of Shard cannot be less than 1");
        }
        if (shard.getId() > ShardUtils.MAX_SHARDS) {
            throw new IllegalArgumentException("ID of Shard cannot be more than " + ShardUtils.MAX_SHARDS);
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

    private Boolean isEnabled(DynamicDataBaseInfo dynamicDataBaseInfo) {
        return Optional.ofNullable(dynamicDataBaseInfo)
                .map(it ->
                        this.isAvailable(it) &&
                                Optional.ofNullable(it.getSegment()).orElse(StringUtils.EMPTY)
                                        .equals(Optional.ofNullable(this.segment).orElse(StringUtils.EMPTY))
                )
                .orElse(true);
    }

    private Boolean isAvailable(DynamicDataBaseInfo dynamicDataBaseInfo) {
        return Optional.ofNullable(dynamicDataBaseInfo)
                .map(it -> it.getAccessible() && it.getAvailable())
                .orElse(true);
    }

    private void runLiquibase(Shard shard, String changeLog) {
        if (isEnabled(shard.getDynamicDataBaseInfo())) {
            try {
                String description = String.format("changelog \"%s\" on shard %s", changeLog, shard.getName());
                log.debug("Run " + description);
                liquibaseManager.runThread(
                        getConnection(shard),
                        shard,
                        changeLog.startsWith(CLASSPATH) ? changeLog.substring(CLASSPATH.length()) : changeLog,
                        shard.getOwner(),
                        description
                );
            } catch (Exception err) {
                throw new RuntimeException(err);
            }
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
                                clusters.forEach((clusterName, cluster) ->
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
                                            })
                                );
                            });
                    runLiquibaseFromPath(path, getDefaultCluster().getMainShard());
                });

        liquibaseManager.waitAll();
    }
}
