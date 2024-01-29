package com.antalex.db.api;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface SQLRunnable {
    void run(Connection connection) throws SQLException;
}
