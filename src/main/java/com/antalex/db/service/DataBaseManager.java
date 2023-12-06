package com.antalex.db.service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public interface DataBaseManager {
    Connection getConnection() throws SQLException;
    DataSource getDataSource();
}
