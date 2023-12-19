package com.antalex.db.service.impl;

import com.antalex.db.service.abstractive.AbstractSequenceGenerator;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

public class ApplicationSequenceGenerator extends AbstractSequenceGenerator {
    private static final String QUERY_LOCK = "SELECT C_VALUE, C_MIN_VALUE, C_CACHE_SIZE\n" +
            "  FROM APP_SEQUENCE\n" +
            "WHERE C_SEQUENCE = ? FOR UPDATE\n";

    private static final String QUERY_UPDATE = "UPDATE APP_SEQUENCE \n" +
            "   SET C_VALUE = ? WHERE C_SEQUENCE = ?";

    private String name;
    private DataSource dataSource;
    private Integer cacheSize;
    private Connection connection;

    public ApplicationSequenceGenerator(String name, DataSource dataSource) {
        this.name = name;
        this.dataSource = dataSource;
    }

    public void setCacheSize(Integer cacheSize) {
        this.cacheSize = cacheSize;
    }

    private Connection getConnection() throws SQLException {
        if (Objects.isNull(this.connection) || this.connection.isClosed()) {
            this.connection = dataSource.getConnection();
            this.connection.setAutoCommit(false);
        }
        return this.connection;
    }

    @Override
    public void init() {
        try {
            Connection connection = getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(QUERY_LOCK);
            preparedStatement.setString(1, this.name);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                this.value = resultSet.getLong(1);
                if (resultSet.wasNull()) {
                    this.value = resultSet.getLong(2);
                }
                this.maxValue = Optional.ofNullable(cacheSize)
                        .map(it -> this.value + it)
                        .orElse(this.value + resultSet.getLong(3));
            }
            preparedStatement = connection.prepareStatement(QUERY_UPDATE);
            preparedStatement.setLong(1, this.maxValue);
            preparedStatement.setString(2, this.name);
            if (preparedStatement.executeUpdate() == 0) {
                throw new RuntimeException(String.format("Ошбка инициализации счетчика \"%s\"", this.name));
            }

            connection.commit();
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }
}
