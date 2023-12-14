package com.antalex.db.service;

import java.sql.Connection;

public interface LiquibaseManager {
    void run(Connection connection, String changeLog);
    void runThread(Connection connection, Object o, String changeLog);
    void wait(Object o);
    void waitAll();
}
