package com.antalex.db.service.impl;

import com.antalex.db.model.Cluster;
import com.antalex.db.service.DataBaseManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ShardDatabaseManager implements DataBaseManager {
    @Autowired
    private Environment env;

    private static final Map<String, Cluster> CLUSTERS = new HashMap<>();
    private static final int MAX_SHARDS = 63;
    private static final int MAX_CLASTERS = 99;

    ShardDatabaseManager() {
        while () {

        }
        env.getProperty("database.driver")

        "dbConfig.clasters[0].shards[0].database.url"
    }
}
