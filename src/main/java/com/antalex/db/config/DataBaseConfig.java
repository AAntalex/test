package com.antalex.db.config;

import lombok.Data;

@Data
public class DataBaseConfig {
    private String driver;
    private String classname;
    private String url;
    private String user;
    private String pass;
    private String owner;
}
