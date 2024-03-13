package com.antalex.db.model.dto;

import com.antalex.db.model.enums.ShardType;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@Builder
public class ClassDto {
    private String classPackage;
    private String targetClassName;
    private String tableName;
    private String cluster;
    private ShardType shardType;
    private List<FieldDto> fields;
    private Map<String, FieldDto> fieldMap;
    private List<IndexDto> indexes;
    private List<FieldDto> uniqueFields;
}
