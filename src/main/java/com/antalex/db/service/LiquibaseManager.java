package com.antalex.db.service;

import liquibase.exception.LiquibaseException;

import java.sql.Connection;

public interface LiquibaseManager {
    void run(Connection connection, String changeLog, String catalogName) throws LiquibaseException;
    void runThread(Connection connection, Object o, String changeLog, String catalogName, String description);
    void wait(Object o);
    void waitAll();
}
