package com.antalex.db.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ClassDto {
    private String className;
    private String classPackage;
    private String targetClassName;
    private String tableName;
    private List<FieldDto> fields;
}
