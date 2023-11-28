package com.antalex.domain.abstraction;

public interface Sharded {
    String getId();
    Long getShardValue();
    Short getCluster();
}
