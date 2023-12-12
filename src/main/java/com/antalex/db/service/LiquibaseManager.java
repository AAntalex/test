package com.antalex.db.service;

import java.sql.Connection;

public interface LiquibaseManager {
    void run(Connection connection, String changeLog);
}
