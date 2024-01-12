package com.antalex.db.utils;

import com.antalex.db.model.Shard;

public class ShardUtils {
    public static final int MAX_SHARDS = 64;
    public static final int MAX_CLUSTERS = 100;
    public static final String DEFAULT_CLUSTER_NAME = "DEFAULT";
    public static final String DEFAULT_OWNER_PREFIX = "$$$";

    public static String transformSQL(String sql, Shard shard) {
        return sql.replace(DEFAULT_OWNER_PREFIX, shard.getOwner());
    }
}
