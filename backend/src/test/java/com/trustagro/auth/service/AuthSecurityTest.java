package com.trustagro.auth.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.auth.dto.LoginRequest;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.entity.UserStatus;
import com.trustagro.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthSecurityTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock UserRepository userRepository;
    @Mock JwtService jwtService;
    @Mock AuditService auditService;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks AuthService authService;

    private LoginRequest req;

    @BeforeEach
    void init() {
        req = new LoginRequest();
        req.setEmail("alice@trustagro.com");
        req.setPassword("password12");
    }

    @AfterEach
    void reset() {
        // rate-limit map is in-memory on the service instance; new inject each test via Mockito
    }

    @Test
    void failedLoginAuditsWithoutPassword() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));

        assertThrows(BadCredentialsException.class, () -> authService.login(req));

        verify(auditService).logSystem(eq("LOGIN_FAILED"), eq("SECURITY"), eq("User"),
                isNull(), eq("alice@trustagro.com"), anyString());
        verify(auditService, never()).log(eq("LOGIN_SUCCESS"), anyString(), anyString(), any(), any(), any());
    }

    @Test
    void inactiveUserDeniedWithGenericMessage() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new DisabledException("disabled"));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> authService.login(req));
        assertEquals("Invalid email or password", ex.getMessage());
        verify(auditService).logSystem(eq("LOGIN_FAILED"), eq("SECURITY"), eq("User"),
                isNull(), eq("alice@trustagro.com"), eq("INACTIVE"));
    }

    @Test
    void rateLimitAfterFiveFailures() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));

        for (int i = 0; i < 5; i++) {
            assertThrows(BadCredentialsException.class, () -> authService.login(req));
        }
        BusinessException limited = assertThrows(BusinessException.class, () -> authService.login(req));
        assertTrue(limited.getMessage().contains("Too many login attempts"));
    }

    @Test
    void customUserDetailsRejectsInactive() {
        CustomUserDetailsService uds = new CustomUserDetailsService(userRepository);
        User user = new User();
        user.setEmail("bob@trustagro.com");
        user.setPassword("hash");
        user.setRole(Role.FINANCE);
        user.setStatus(UserStatus.INACTIVE);
        when(userRepository.findByEmail("bob@trustagro.com")).thenReturn(Optional.of(user));

        var details = uds.loadUserByUsername("bob@trustagro.com");
        assertFalse(details.isEnabled());
    }
}
