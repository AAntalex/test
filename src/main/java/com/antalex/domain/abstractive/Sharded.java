package com.antalex.domain.abstractive;

public interface Sharded {
    String getId();
    Long getShardValue();
    Short getCluster();
}
