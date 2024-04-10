package com.antalex.db.entity;

import com.antalex.db.entity.abstraction.BaseShardEntity;
import com.antalex.db.model.enums.DataFormat;
import lombok.Data;

@Data
public class DomainStorage extends BaseShardEntity {
    private Long entityId;
    private String storageName;
    private String data;
    private DataFormat dataFormat;
}
