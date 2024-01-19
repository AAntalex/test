package com.antalex.db.config;

import lombok.Data;

@Data
public class ShardConfig {
    private Short id;
    private Boolean main;
    private String segment;
    private DataBaseConfig dataBase;
    private HikariSettings hikari;
}
