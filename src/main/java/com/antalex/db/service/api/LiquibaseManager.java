package com.antalex.db.service.api;

import liquibase.exception.LiquibaseException;

import java.sql.Connection;

public interface LiquibaseManager {
    void run(Connection connection, String changeLog, String catalogName) throws LiquibaseException;
}
