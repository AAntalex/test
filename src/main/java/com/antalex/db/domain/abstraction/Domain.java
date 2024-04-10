package com.antalex.db.domain.abstraction;

import com.antalex.db.entity.abstraction.ShardInstance;

public interface Domain {
    <T extends ShardInstance> void setEntity(T entity);
}
