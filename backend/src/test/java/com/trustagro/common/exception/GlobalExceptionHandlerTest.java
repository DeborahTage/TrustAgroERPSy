package com.trustagro.common.exception;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.config.CorrelationIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void dataIntegrityMapsToConflictWithoutSql() {
        MDC.put(CorrelationIdFilter.MDC_KEY, "rid-conflict");
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "could not execute statement",
                new RuntimeException("ERROR: duplicate key value violates unique constraint \"users_email_key\""));

        ResponseEntity<ApiResponse<Void>> response = handler.handleConflict(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Conflict: resource already exists or violates a constraint", response.getBody().getMessage());
        assertFalse(response.getBody().getMessage().toLowerCase().contains("duplicate key"));
        assertEquals("rid-conflict", response.getBody().getRequestId());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void unreadableBodyMapsToBadRequest() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error");
        ResponseEntity<ApiResponse<Void>> response = handler.handleUnreadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Malformed or unreadable request body", response.getBody().getMessage());
    }
}
