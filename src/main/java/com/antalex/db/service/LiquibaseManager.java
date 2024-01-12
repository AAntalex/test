package com.antalex.db.service;

import java.sql.Connection;

public interface LiquibaseManager {
    void run(Connection connection, String changeLog, String catalogName);
    void runThread(Connection connection, Object o, String changeLog, String catalogName);
    void wait(Object o);
    void waitAll();
}
