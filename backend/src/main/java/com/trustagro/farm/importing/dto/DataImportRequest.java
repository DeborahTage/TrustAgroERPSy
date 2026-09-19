package com.trustagro.farm.importing.dto;

import lombok.Data;

@Data
public class DataImportRequest {
    /** FARMS | BATCHES | INVENTORY_ITEMS */
    private String type;
    private String csv;
    private boolean dryRun = true;
    private boolean confirm = false;
}
