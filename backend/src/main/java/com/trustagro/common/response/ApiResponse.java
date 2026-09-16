package com.trustagro.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trustagro.config.CorrelationIdFilter;
import lombok.Data;
import org.slf4j.MDC;

import java.time.Instant;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String requestId;
    private Instant timestamp;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = true;
        r.data = data;
        enrich(r);
        return r;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = true;
        r.message = message;
        r.data = data;
        enrich(r);
        return r;
    }

    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = false;
        r.message = message;
        enrich(r);
        return r;
    }

    private static void enrich(ApiResponse<?> r) {
        r.timestamp = Instant.now();
        String requestId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (requestId != null && !requestId.isBlank()) {
            r.requestId = requestId;
        }
    }

    public void setSuccess(boolean success) { this.success = success; }
    public void setMessage(String message) { this.message = message; }
    public void setData(T data) { this.data = data; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
