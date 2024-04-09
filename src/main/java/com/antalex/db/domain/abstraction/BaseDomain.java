package com.antalex.db.domain.abstraction;

import com.antalex.db.entity.abstraction.ShardInstance;

public abstract class BaseDomain implements Domain {
    protected ShardInstance entity;
}
