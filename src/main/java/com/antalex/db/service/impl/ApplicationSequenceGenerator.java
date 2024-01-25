package com.antalex.db.service.impl;

import com.antalex.db.model.Shard;
import com.antalex.db.service.abstractive.AbstractSequenceGenerator;
import com.antalex.db.utils.ShardUtils;
import com.antalex.profiler.service.ProfilerService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

public class ApplicationSequenceGenerator extends AbstractSequenceGenerator {
    private static final String QUERY_LOCK = "SELECT LAST_VALUE,MIN_VALUE,CACHE_SIZE,MAX_VALUE,CYCLE_FLAG\n" +
            "  FROM $$$.APP_SEQUENCE\n" +
            "WHERE SEQUENCE_NAME = ? FOR UPDATE";

    private static final String QUERY_UPDATE = "UPDATE $$$.APP_SEQUENCE SET LAST_VALUE = ? WHERE SEQUENCE_NAME = ?";

    private String name;
    private Shard shard;
    private Integer cacheSize;
    private Connection connection;


    private ProfilerService profiler;
    public void setProfiler(ProfilerService profiler) {
        this.profiler = profiler;
    }
    private void startTimeCounter(String name, String method) {
        if (Objects.nonNull(profiler)) {
            profiler.startTimeCounter(name, method);
        }
    }
    private void fixTimeCounter() {
        if (Objects.nonNull(profiler)) {
            profiler.fixTimeCounter();
        }
    }

    public ApplicationSequenceGenerator(String name, Shard shard) {
        this.name = name;
        this.shard = shard;
    }

    public void setCacheSize(Integer cacheSize) {
        this.cacheSize = cacheSize;
    }

    private Connection getConnection() throws SQLException {
        startTimeCounter("getConnection", "AAA");
        if (Objects.isNull(this.connection) || this.connection.isClosed()) {
            this.connection = shard.getDataSource().getConnection();
            this.connection.setAutoCommit(false);
        }
        fixTimeCounter();
        return this.connection;
    }

    private void closeConnection() {
        startTimeCounter("closeConnection", "AAA");
        try {
            if (Objects.nonNull(this.connection) && !this.connection.isClosed()) {
                this.connection.close();
            }
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
        fixTimeCounter();
    }

    @Override
    public void init() {
        startTimeCounter("init", "AAA");
        try {
            Connection connection = getConnection();
            startTimeCounter("preparedStatement", "AAA");
            PreparedStatement preparedStatement = connection.prepareStatement(
                    ShardUtils.transformSQL(QUERY_LOCK, this.shard)
            );
            preparedStatement.setString(1, this.name);
            fixTimeCounter();
            startTimeCounter("executeQuery", "AAA");
            ResultSet resultSet = preparedStatement.executeQuery();
            fixTimeCounter();
            startTimeCounter("resultSet", "AAA");
            if (resultSet.next()) {
                this.value = resultSet.getLong(1);
                if (resultSet.wasNull()) {
                    this.value = resultSet.getLong(2);
                }
                this.maxValue = Optional.ofNullable(cacheSize)
                        .map(it -> this.value + it)
                        .orElse(this.value + Long.max(resultSet.getLong(3), 1L));

                startTimeCounter("resultSet QUERY_UPDATE prepare", "AAA");
                preparedStatement = connection.prepareStatement(
                        ShardUtils.transformSQL(QUERY_UPDATE, this.shard)
                );

                Long sequenceMaxValue = resultSet.getLong(4);
                if (!resultSet.wasNull() && this.maxValue.compareTo(sequenceMaxValue) > 0) {
                    this.maxValue = sequenceMaxValue;
                    if (resultSet.getBoolean(5)) {
                        preparedStatement.setLong(1, resultSet.getLong(2));
                    } else {
                        if (this.value.compareTo(this.maxValue) >= 0) {
                            connection.rollback();
                            throw new RuntimeException(
                                    String.format(
                                            "Достигли придельнго значения счетчика \"%s\" - %d",
                                            this.name,
                                            this.maxValue
                                    )
                            );
                        }
                    }
                } else {
                    preparedStatement.setLong(1, this.maxValue);
                }
                preparedStatement.setString(2, this.name);
                fixTimeCounter();
                startTimeCounter("resultSet QUERY_UPDATE executeUpdate", "AAA");
                if (preparedStatement.executeUpdate() == 0) {
                    connection.rollback();
                    throw new RuntimeException(String.format("Ошбка инициализации счетчика \"%s\"", this.name));
                }
                fixTimeCounter();
            }
            fixTimeCounter();
            startTimeCounter("commit", "AAA");
            connection.commit();
            fixTimeCounter();
        } catch (Exception err) {
            throw new RuntimeException(err);
        } finally {
            closeConnection();
        }
        fixTimeCounter();
    }
}
