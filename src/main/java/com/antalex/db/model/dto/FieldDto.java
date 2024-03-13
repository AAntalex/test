package com.antalex.db.model.dto;

import lombok.Builder;
import lombok.Data;

import javax.lang.model.element.Element;

@Data
@Builder
public class FieldDto {
    private String fieldName;
    private Element element;
    private String columnName;
    private String getter;
    private String setter;
    private Boolean isLinked;
    private boolean isUnique;
}
