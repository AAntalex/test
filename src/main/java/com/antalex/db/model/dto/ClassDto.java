package com.antalex.db.model.dto;

import com.antalex.db.model.enums.ShardType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ClassDto {
    private String classPackage;
    private String targetClassName;
    private String tableName;
    private String cluster;
    private ShardType shardType;
    private List<FieldDto> fields;
}
