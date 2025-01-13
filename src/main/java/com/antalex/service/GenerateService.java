package com.antalex.service;


import com.antalex.db.entity.abstraction.ShardInstance;

import java.util.List;

public interface GenerateService<T extends ShardInstance> {
    List<T> generate(int cnt, int cntClient, int cntAccount);
}
