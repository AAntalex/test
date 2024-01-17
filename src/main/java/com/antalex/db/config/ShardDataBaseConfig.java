package com.antalex.db.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties("dbconfig")
public class ShardDataBaseConfig {
    private String clusters;
}

class Cluster {
    private Short id;
    private String name;
    private Boolean defaultCluster;
    private List<Shard> shards;
}

class Shard {
    private Short id;
    private Boolean main;
    private DataBase dataBase;
}

class DataBase {
    private String driver;
    private String url;
    private String user;
    private String pass;
    private String owner;
}
