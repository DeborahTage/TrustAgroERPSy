package com.trustagro.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustagro.audit.entity.AuditLog;
import com.trustagro.audit.repository.AuditLogRepository;
import com.trustagro.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditServiceTest {

    @Mock AuditLogRepository auditLogRepository;
    @Mock UserRepository userRepository;

    AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditLogRepository, userRepository, new ObjectMapper());
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void logPersistsSynchronouslyViaRepository() {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("admin@trustagro.com", "x", "ROLE_ADMIN");
        SecurityContextHolder.getContext().setAuthentication(auth);

        auditService.log("CREATE", "INVENTORY", "Item", 9L, null, "{\"ok\":true}");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals("CREATE", saved.getAction());
        assertEquals("INVENTORY", saved.getModule());
        assertEquals("admin@trustagro.com", saved.getUserEmail());
        assertEquals("SUCCESS", saved.getResult());
    }

    @Test
    void redactsPasswordAndTokenKeysBeforeSave() {
        auditService.log("UPDATE", "USER", "User", 1L,
                "{\"password\":\"secret123\",\"name\":\"a\"}",
                "{\"token\":\"abc\",\"jwt\":\"xyz\",\"otp\":\"111111\"}");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertFalse(saved.getOldValue().contains("secret123"));
        assertTrue(saved.getOldValue().contains("[REDACTED]"));
        assertFalse(saved.getNewValue().contains("abc"));
        assertTrue(saved.getNewValue().contains("[REDACTED]"));
    }

    @Test
    void logSystemUsesAttemptedEmailWithoutPassword() {
        auditService.logSystem("LOGIN_FAILED", "SECURITY", "User", null,
                "user@example.com", "BAD_CREDENTIALS");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals("user@example.com", captor.getValue().getUserEmail());
        assertEquals("FAILURE", captor.getValue().getResult());
        assertFalse(String.valueOf(captor.getValue().getNewValue()).toLowerCase().contains("password"));
    }
}
