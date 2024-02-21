package com.antalex.db.config;

import lombok.Data;

@Data
public class ShardConfig {
    private Short id;
    private Boolean main;
    private DataBaseConfig dataBase;
    private HikariSettings hikari;
    private SharedTransactionConfig transactionConfig;
    private Integer sequenceCacheSize;
    private String segment;
    private Boolean accessible;
    private String url;
}
