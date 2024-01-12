package com.antalex.db.service.impl;

import com.antalex.db.service.LiquibaseManager;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class LiquibaseManagerImpl implements LiquibaseManager {
    private Map<Object, Pair<Thread, String>> liquibaseRuns = new HashMap<>();

    @Override
    public void run(Connection connection, String changeLog, String catalogName) {
        try {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(
                    new JdbcConnection(connection)
            );
            database.setDefaultCatalogName(catalogName);
            Liquibase liquibase = new Liquibase(
                    changeLog,
                    new ClassLoaderResourceAccessor(),
                    database

            );

            liquibase.update(new Contexts(), new LabelExpression());

        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    @Override
    public void wait(Object o) {
        Optional.ofNullable(o)
                .map(liquibaseRuns::get)
                .ifPresent(it -> {
                    try {
                        log.info(
                                String.format(
                                        "Waiting thread %s for changelog \"%s\"...",
                                        it.getKey().getName(),
                                        it.getValue()
                                )
                        );
                        it.getKey().join();
                        liquibaseRuns.remove(o);
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
    public void runThread(Connection connection, Object o, String changeLog, String catalogName) {
        Thread thread = new Thread(() -> {
            try {
                run(connection, changeLog, catalogName);
            } catch (Exception err) {
                throw new RuntimeException(err);
            }
        });
        this.wait(o);
        liquibaseRuns.put(o, ImmutablePair.of(thread, changeLog));
        thread.start();
    }

}
