package com.trustagro.farm.importing.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class DataImportResult {
    private String type;
    private boolean dryRun;
    private int rowsFound;
    private int valid;
    private int invalid;
    private int duplicates;
    private int missingRefs;
    private int willCreate;
    private int willUpdate;
    private int created;
    private int updated;
    private List<String> errors = new ArrayList<>();
    private Map<String, Object> expectedCounts = new LinkedHashMap<>();
}
