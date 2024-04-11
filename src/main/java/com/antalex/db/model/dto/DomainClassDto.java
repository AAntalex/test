package com.antalex.db.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DomainClassDto {
    private String classPackage;
    private String targetClassName;
    private EntityClassDto entityClass;
    private List<DomainFieldDto> fields;
}
