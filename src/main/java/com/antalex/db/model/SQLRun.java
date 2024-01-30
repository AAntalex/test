package com.antalex.db.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SQLRun {
    private List<SQLRunInfo> runs = new ArrayList<>();
    private String errorMessage;
    private Boolean hasError = false;
    private Boolean needCommit = false;
}
