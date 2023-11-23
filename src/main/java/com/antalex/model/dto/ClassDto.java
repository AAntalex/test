package com.antalex.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ClassDto {
    private String className;
    private String classPackage;
    private List<FieldDto> fields;
}
