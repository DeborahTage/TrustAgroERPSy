package com.trustagro.config;

import com.trustagro.auth.service.CustomUserDetailsService;
import com.trustagro.auth.service.JwtService;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.entity.UserStatus;
import com.trustagro.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    private static final String SECRET = "GeneratedTestJwtSecretForFilterVerification123456789";

    private JwtService jwtService;
    private CustomUserDetailsService userDetailsService;
    private UserRepository userRepository;
    private JwtAuthFilter filter;
    private User user;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", 60_000L);
        userDetailsService = mock(CustomUserDetailsService.class);
        userRepository = mock(UserRepository.class);
        filter = new JwtAuthFilter(jwtService, userDetailsService, userRepository);

        user = new User();
        user.setId(44L);
        user.setEmail("user@trustagro.test");
        user.setRole(Role.PHARMACIST);
        user.setStatus(UserStatus.ACTIVE);
        user.setTokenVersion(2L);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userDetailsService.toUserDetails(any(User.class)))
                .thenAnswer(inv -> details(((User) inv.getArgument(0)).getStatus() != UserStatus.INACTIVE));
        when(userDetailsService.loadUserByUsername(user.getEmail()))
                .thenAnswer(inv -> details(user.getStatus() != UserStatus.INACTIVE));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void activeUserWithCurrentVersionIsAuthenticated() throws Exception {
        String token = token(2L, 60_000L);

        runFilter(token);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(user.getEmail(), SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void deletedUserWithOtherwiseValidJwtDoesNotThrow500AndIsRejected() throws Exception {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());
        when(userDetailsService.loadUserByUsername(user.getEmail()))
                .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found"));

        runFilter(token(2L, 60_000L));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(userDetailsService, never()).toUserDetails(any(User.class));
    }

    @Test
    void inactiveUserTokenIsRejected() throws Exception {
        user.setStatus(UserStatus.INACTIVE);

        runFilter(token(2L, 60_000L));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void passwordChangeOrLogoutVersionRevokesPreviouslyIssuedToken() throws Exception {
        runFilter(token(1L, 60_000L));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        runFilter(token(2L, -1_000L));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void malformedTokenIsRejected() throws Exception {
        runFilter("not-a-valid-jwt-token");

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private UserDetails details(boolean enabled) {
        return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password("encoded")
                .authorities(List.of())
                .disabled(!enabled)
                .build();
    }

    private String token(long version, long expiresInMillis) {
        ReflectionTestUtils.setField(jwtService, "expiration", expiresInMillis);
        return jwtService.generateToken(user.getEmail(), "PHARMACIST", List.of(), List.of(), version);
    }

    private void runFilter(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));
    }
}
