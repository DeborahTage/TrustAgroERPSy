package com.trustagro.auth.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.auth.dto.ChangePasswordRequest;
import com.trustagro.auth.dto.LoginRequest;
import com.trustagro.auth.dto.LoginResponse;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.common.web.RequestContext;
import com.trustagro.security.RoleCompatibility;
import com.trustagro.security.RolePermissionCatalog;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.entity.UserStatus;
import com.trustagro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authentication and credential management service.
 *
 * <p>Rate Limiting Note (Single-Instance Limitation):
 * Failed login attempts are tracked in-process using an eviction-bounded {@link ConcurrentHashMap}.
 * In a multi-replica or clustered deployment, rate-limiting state is not shared across application
 * instances. Before horizontal scaling, front this service with a shared rate limiter such as
 * Redis (token-bucket) or ingress reverse-proxy rate limiting (e.g. Nginx {@code limit_req_zone}).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String MODULE = "SECURITY";
    private static final String GENERIC_LOGIN_ERROR = "Invalid email or password";
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long WINDOW_MS = 15 * 60 * 1000L;

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    private final Map<String, List<Long>> failedAttempts = new ConcurrentHashMap<>();

    public LoginResponse login(LoginRequest req) {
        String email = req.getEmail() != null ? req.getEmail().trim().toLowerCase() : "";
        String ip = RequestContext.currentIpAddress();
        assertNotRateLimited(email, ip);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        } catch (DisabledException ex) {
            recordFailure(email, ip);
            auditService.logSystem("LOGIN_FAILED", MODULE, "User", null, email, "INACTIVE");
            throw new BadCredentialsException(GENERIC_LOGIN_ERROR);
        } catch (BadCredentialsException ex) {
            recordFailure(email, ip);
            auditService.logSystem("LOGIN_FAILED", MODULE, "User", null, email, "BAD_CREDENTIALS");
            throw new BadCredentialsException(GENERIC_LOGIN_ERROR);
        } catch (AuthenticationException ex) {
            recordFailure(email, ip);
            auditService.logSystem("LOGIN_FAILED", MODULE, "User", null, email, "AUTH_FAILED");
            throw new BadCredentialsException(GENERIC_LOGIN_ERROR);
        }

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BadCredentialsException(GENERIC_LOGIN_ERROR));
        if (user.getStatus() == UserStatus.INACTIVE) {
            recordFailure(email, ip);
            auditService.logSystem("LOGIN_FAILED", MODULE, "User", user.getId(), email, "INACTIVE");
            throw new BadCredentialsException(GENERIC_LOGIN_ERROR);
        }

        clearFailures(email, ip);

        Role canonicalRole = RoleCompatibility.normalize(user.getRole());
        List<String> permissions = RolePermissionCatalog.permissionsFor(canonicalRole);
        List<Long> farmIds = user.getFarmIds() != null ? user.getFarmIds() : List.of();

        String token = jwtService.generateToken(user.getEmail(), canonicalRole.name(), permissions, farmIds,
                user.getTokenVersion() != null ? user.getTokenVersion() : 0L);
        auditService.log("LOGIN_SUCCESS", MODULE, "User", user.getId(), null, user.getEmail());
        return new LoginResponse(token, user.getId(), user.getFullName(), user.getEmail(),
                canonicalRole, permissions, farmIds);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect");
        }
        if (req.getNewPassword() == null || req.getNewPassword().length() < 8) {
            throw new BusinessException("New password must be at least 8 characters");
        }
        if (passwordEncoder.matches(req.getNewPassword(), user.getPassword())) {
            throw new BusinessException("New password must differ from the current password");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        long currentVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0L;
        user.setTokenVersion(currentVersion + 1L);
        userRepository.save(user);
        auditService.log("PASSWORD_CHANGE", MODULE, "User", user.getId(), null, "password-updated");
    }

    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));
        long currentVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0L;
        user.setTokenVersion(currentVersion + 1L);
        userRepository.save(user);
        auditService.log("LOGOUT", MODULE, "User", user.getId(), null, "User JWTs revoked");
    }

    private void assertNotRateLimited(String email, String ip) {
        if (isLimited("email:" + email) || (ip != null && isLimited("ip:" + ip))) {
            throw new BusinessException("Too many login attempts. Try again later.");
        }
    }

    private boolean isLimited(String key) {
        prune(key);
        List<Long> attempts = failedAttempts.get(key);
        if (attempts == null) {
            return false;
        }
        synchronized (attempts) {
            return attempts.size() >= MAX_FAILED_ATTEMPTS;
        }
    }

    private void recordFailure(String email, String ip) {
        long now = Instant.now().toEpochMilli();
        addAttempt("email:" + email, now);
        if (ip != null && !ip.isBlank()) {
            addAttempt("ip:" + ip, now);
        }
        cleanupIfOversized();
    }

    private void addAttempt(String key, long timestamp) {
        List<Long> attempts = failedAttempts.computeIfAbsent(key, k -> new ArrayList<>());
        synchronized (attempts) {
            attempts.add(timestamp);
        }
        prune(key);
    }

    private void cleanupIfOversized() {
        if (failedAttempts.size() > 1000) {
            long cutoff = Instant.now().toEpochMilli() - WINDOW_MS;
            failedAttempts.entrySet().removeIf(entry -> {
                List<Long> list = entry.getValue();
                synchronized (list) {
                    list.removeIf(time -> time < cutoff);
                    return list.isEmpty();
                }
            });
        }
    }

    private void clearFailures(String email, String ip) {
        failedAttempts.remove("email:" + email);
        if (ip != null) {
            failedAttempts.remove("ip:" + ip);
        }
    }

    private void prune(String key) {
        List<Long> attempts = failedAttempts.get(key);
        if (attempts == null) {
            return;
        }
        long cutoff = Instant.now().toEpochMilli() - WINDOW_MS;
        synchronized (attempts) {
            Iterator<Long> it = attempts.iterator();
            while (it.hasNext()) {
                if (it.next() < cutoff) {
                    it.remove();
                }
            }
            if (attempts.isEmpty()) {
                failedAttempts.remove(key, attempts);
            }
        }
    }
}
