package com.antalex.db.service.impl;

import com.antalex.db.service.api.LiquibaseManager;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;

@Slf4j
public class LiquibaseManagerImpl implements LiquibaseManager {
    @Override
    public void run(Connection connection, String changeLog, String catalogName) throws LiquibaseException {
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(
                new JdbcConnection(connection)
        );
        database.setDefaultCatalogName(catalogName);
        database.setDefaultSchemaName(catalogName);
        Liquibase liquibase = new Liquibase(
                changeLog,
                new ClassLoaderResourceAccessor(),
                database

        );
        liquibase.update(new Contexts(), new LabelExpression());
    }
}
