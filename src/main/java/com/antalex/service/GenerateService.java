package com.antalex.service;


import com.antalex.db.entity.abstraction.ShardInstance;

import java.util.List;

public interface GenerateService<T extends ShardInstance> {
    List<T> generate(String accountPrefix, int cnt, int cntAccount, int cntClient);
}
