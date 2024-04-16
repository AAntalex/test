package com.antalex.db.domain.abstraction;

import com.antalex.db.entity.AttributeStorage;
import com.antalex.db.entity.abstraction.ShardInstance;

import java.util.Map;

public interface Domain {
    <T extends ShardInstance> T getEntity();
    Map<String, AttributeStorage> getStorage();
}
