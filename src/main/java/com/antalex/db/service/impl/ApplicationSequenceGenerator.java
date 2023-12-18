package com.antalex.db.service.impl;

import com.antalex.db.service.abstractive.AbstractSequenceGenerator;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ApplicationSequenceGenerator extends AbstractSequenceGenerator {
    private static final String QUERY = "UPDATE APP_SEQUENCE \n" +
            "   SET С_VALUE = COALESCE(С_VALUE, С_MIN_VALUE) + COALESCE(?, C_CACHE_SIZE)\n" +
            "WHERE С_SEQUENCE = ?";

    private String name;
    private DataSource dataSource;
    private Integer cacheSize;

    ApplicationSequenceGenerator(String name, DataSource dataSource) {
        this.name = name;
        this.dataSource = dataSource;
    }

    public void setCacheSize(Integer cacheSize) {
        this.cacheSize = cacheSize;
    }

    @Override
    public void init() {
        try {
            Connection connection = dataSource.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(String.format(QUERY, this.name));

            preparedStatement.setInt(1, this.cacheSize);
            preparedStatement.setString(2, this.name);

            ResultSet resultSet = preparedStatement.executeQuery();

        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }
}
