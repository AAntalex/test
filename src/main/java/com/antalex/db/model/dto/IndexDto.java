package com.antalex.db.model.dto;

import lombok.Builder;
import lombok.Data;

import javax.lang.model.element.Element;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class IndexDto {
    private String name;
    private Boolean unique;
    private String columnList;
}
