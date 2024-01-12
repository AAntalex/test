package com.antalex.db.model;

import lombok.Data;

import javax.sql.DataSource;

@Data
public class Shard {
    private Short id;
    private DataSource dataSource;
    private DataBaseInfo dataBaseInfo;
    private String owner;
}
