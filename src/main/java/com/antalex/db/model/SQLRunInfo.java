package com.antalex.db.model;

import lombok.Data;

import java.sql.Connection;

@Data
public class SQLRunInfo {
    private Connection connection;
    private Thread thread;
    private String error;
}
