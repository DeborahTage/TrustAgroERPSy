package com.trustagro.governance;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ControlCheckResult {
    private String code;
    private String title;
    private String status; // OK | WARNING | ERROR | UNAVAILABLE
    private String message;
    private long count;
}
