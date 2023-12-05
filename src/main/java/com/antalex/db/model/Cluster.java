package com.antalex.db.model;

import com.antalex.db.service.SequenceGenerator;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Cluster {
    private Short id;
    private String name;
    private List<Shard> shards = new ArrayList<>();
    private SequenceGenerator shardSequence;
}
