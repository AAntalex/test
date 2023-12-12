package com.antalex.db.service.impl;

import com.antalex.db.service.LiquibaseManager;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.stereotype.Component;

import java.sql.Connection;

@Component
public class LiquibaseManagerImpl implements LiquibaseManager {

    @Override
    public void run(Connection connection, String changeLog) {
        try {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(
                    new JdbcConnection(connection)
            );
            Liquibase liquibase = new liquibase.Liquibase(
                    changeLog,
                    new ClassLoaderResourceAccessor(),
                    database

            );
            liquibase.update(new Contexts(), new LabelExpression());
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }
}
