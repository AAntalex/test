package com.antalex.db.service.impl;

import com.antalex.db.model.SQLRunInfo;
import com.antalex.db.service.LiquibaseManager;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class LiquibaseManagerImpl implements LiquibaseManager {
    private Map<Object, SQLRunInfo> liquibaseRuns = new HashMap<>();

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

    @Override
    public void wait(Object o) {
        Optional.ofNullable(o)
                .map(liquibaseRuns::get)
                .ifPresent(it -> {
                    try {
                        log.debug(
                                String.format(
                                        "Waiting %s for %s...",
                                        it.getThread().getName(),
                                        it.getDescription()
                                )
                        );
                        it.getThread().join();
                        liquibaseRuns.remove(o);
                        if (Objects.nonNull(it.getError())) {
                            throw new RuntimeException(
                                    String.format(
                                            "Error executing %s: %s",
                                            it.getDescription(),
                                            it.getError()
                                    )
                            );
                        }
                    } catch (InterruptedException err) {
                        throw new RuntimeException(err);
                    }
                });
    }

    @Override
    public void waitAll() {
        while (!liquibaseRuns.isEmpty()) {
            this.wait(
                    liquibaseRuns.entrySet()
                            .stream()
                            .findAny()
                            .map(Map.Entry::getKey)
                            .orElse(null)
            );
        }
    }

    @Override
    public void runThread(Connection connection, Object o, String changeLog, String catalogName, String description) {
        SQLRunInfo sqlRunInfo = new SQLRunInfo();
        Thread thread = new Thread(() -> {
            try {
                sqlRunInfo.setConnection(connection);
                run(connection, changeLog, catalogName);
            } catch (LiquibaseException err) {
                sqlRunInfo.setError(err.getLocalizedMessage());
                throw new RuntimeException(err);
            }
        });
        sqlRunInfo.setThread(thread);
        sqlRunInfo.setDescription(description);
        this.wait(o);
        liquibaseRuns.put(o, sqlRunInfo);
        thread.start();
    }
}
