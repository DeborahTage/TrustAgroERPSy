package com.trustagro.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.trustagro.audit.entity.AuditLog;
import com.trustagro.audit.repository.AuditLogRepository;
import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.common.web.RequestContext;
import com.trustagro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    public static final String RESULT_SUCCESS = "SUCCESS";
    public static final String RESULT_FAILURE = "FAILURE";

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "token", "jwt", "secret", "otp",
            "currentpassword", "newpassword", "confirmpassword", "accesstoken", "refreshtoken"
    );

    private static final Pattern SENSITIVE_JSON_KEY = Pattern.compile(
            "\"(password|token|jwt|secret|otp|currentPassword|newPassword|confirmPassword|accessToken|refreshToken)\"\\s*:\\s*(\"(?:\\\\.|[^\"\\\\])*\"|[^,}\\]]+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final List<String> SECURITY_ACTIONS = List.of(
            "LOGIN_FAILED", "LOGIN_SUCCESS", "LOGOUT", "PASSWORD_CHANGE",
            "ROLE_CHANGE", "USER_DISABLED", "PERMISSION"
    );

    private static final List<String> HIGH_RISK_ACTIONS = List.of(
            "LOGIN_FAILED", "PASSWORD_CHANGE", "ROLE_CHANGE", "USER_DISABLED",
            "PERMISSION", "FINANCE_POSTED", "PROC_PO_APPROVED", "PROC_SINV_APPROVED"
    );

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * Log a simple string-based audit entry.
     */
    @Transactional
    public void log(String action, String module, Long recordId, String oldValue, String newValue) {
        log(action, module, null, recordId, oldValue, newValue);
    }

    /**
     * Public entry point for logging. Captures current user context.
     * Writes synchronously in the caller's transaction so rollbacks remove false success audits.
     */
    @Transactional
    public void log(String action, String module, String entityType, Long entityId, String oldValue, String newValue) {
        String email = "System";
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getName() != null
                    && !"anonymousUser".equals(auth.getName())) {
                email = auth.getName();
            }
        } catch (Exception ignored) {
            // keep System
        }
        persist(action, module, entityType, entityId, oldValue, newValue, email, RESULT_SUCCESS);
    }

    /**
     * Unauthenticated / system events (e.g. failed login). Never pass passwords.
     */
    @Transactional
    public void logSystem(String action, String module, String entityType, Long entityId,
                          String oldValue, String newValue) {
        String email = oldValue != null && oldValue.contains("@") ? oldValue : "System";
        // Prefer explicit attempted email in newValue for LOGIN_FAILED when old is null
        if (newValue != null && newValue.contains("@") && !newValue.contains("{")) {
            email = newValue;
        }
        String result = action != null && action.toUpperCase(Locale.ROOT).contains("FAIL")
                ? RESULT_FAILURE : RESULT_SUCCESS;
        persist(action, module, entityType, entityId, oldValue, newValue, email, result);
    }

    /**
     * Convenience method for logging object state as JSON.
     */
    @Transactional
    public void logObject(String action, String module, String entityType, Long entityId,
                          Object oldObj, Object newObj) {
        try {
            String oldVal = oldObj != null ? objectMapper.writeValueAsString(oldObj) : null;
            String newVal = newObj != null ? objectMapper.writeValueAsString(newObj) : null;
            log(action, module, entityType, entityId, oldVal, newVal);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize audit log objects for {}/{}", entityType, entityId);
            log(action, module, entityType, entityId, null, null);
        }
    }

    private void persist(String action, String module, String entityType, Long entityId,
                         String oldValue, String newValue, String email, String result) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setModule(module != null ? module : "SYSTEM");
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setOldValue(redactSensitive(oldValue));
        auditLog.setNewValue(redactSensitive(newValue));
        auditLog.setUserEmail(email);
        auditLog.setResult(result != null ? result : RESULT_SUCCESS);
        auditLog.setIpAddress(RequestContext.currentIpAddress());

        try {
            if (email != null && !"System".equals(email)) {
                userRepository.findByEmail(email).ifPresent(u -> auditLog.setUserId(u.getId()));
            }
        } catch (Exception e) {
            log.warn("Failed to find user for audit log: {}", email);
        }

        auditLogRepository.save(auditLog);
    }

    /**
     * Strip password/token/jwt/secret/otp keys from JSON-ish strings before persistence.
     */
    String redactSensitive(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                JsonNode node = objectMapper.readTree(trimmed);
                redactNode(node);
                return objectMapper.writeValueAsString(node);
            } catch (Exception ignored) {
                // fall through to regex
            }
        }
        Matcher m = SENSITIVE_JSON_KEY.matcher(value);
        return m.replaceAll("\"$1\":\"[REDACTED]\"");
    }

    private void redactNode(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            Iterator<String> names = obj.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                if (SENSITIVE_KEYS.contains(name.toLowerCase(Locale.ROOT))) {
                    obj.put(name, "[REDACTED]");
                } else {
                    redactNode(obj.get(name));
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                redactNode(child);
            }
        }
    }

    public List<AuditLog> getAll() {
        return auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public List<AuditLog> getByModule(String module) {
        return auditLogRepository.findByModuleOrderByCreatedAtDesc(module);
    }

    public List<AuditLog> getWithFilters(String module, String entityType, String action,
                                          Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.findWithFilters(module, entityType, action, userId,
                fromOrSentinel(startDate), toOrSentinel(endDate));
    }

    /**
     * PostgreSQL cannot infer the JDBC type of a bound null timestamp in
     * {@code (:startDate IS NULL OR a.createdAt >= :startDate)}. Always pass a real
     * timestamp so Hibernate does not emit an untyped null.
     */
    private static final LocalDateTime SEARCH_FROM_SENTINEL = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime SEARCH_TO_SENTINEL = LocalDateTime.of(2999, 12, 31, 23, 59, 59);

    private static LocalDateTime fromOrSentinel(LocalDateTime startDate) {
        return startDate != null ? startDate : SEARCH_FROM_SENTINEL;
    }

    private static LocalDateTime toOrSentinel(LocalDateTime endDate) {
        return endDate != null ? endDate : SEARCH_TO_SENTINEL;
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> search(String module, String action, String entityType, Long entityId,
                                 Long userId, LocalDateTime startDate, LocalDateTime endDate,
                                 String keyword, Pageable pageable) {
        Pageable sorted = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        LocalDateTime from = fromOrSentinel(startDate);
        LocalDateTime to = toOrSentinel(endDate);
        if (kw == null) {
            return auditLogRepository.search(module, entityType, action, entityId, userId,
                    from, to, sorted);
        }
        return auditLogRepository.searchWithKeyword(module, entityType, action, entityId, userId,
                from, to, kw, sorted);
    }

    @Transactional(readOnly = true)
    public AuditLog getById(Long id) {
        return auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Audit log not found: " + id));
    }

    @Transactional(readOnly = true)
    public long countToday() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        return auditLogRepository.countByCreatedAtGreaterThanEqual(start);
    }

    @Transactional(readOnly = true)
    public long countByActionSince(String action, LocalDateTime since) {
        return auditLogRepository.countByActionAndCreatedAtGreaterThanEqual(action, since);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> securityEvents(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        Pageable sorted = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return auditLogRepository.findSecurityEvents(
                SECURITY_ACTIONS, fromOrSentinel(startDate), toOrSentinel(endDate), sorted);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> recentHighRisk(int limit) {
        int size = Math.max(1, Math.min(limit, 50));
        return auditLogRepository.findRecentByActions(HIGH_RISK_ACTIONS, PageRequest.of(0, size));
    }

    @Transactional(readOnly = true)
    public long failedLoginsSince(LocalDateTime since) {
        return countByActionSince("LOGIN_FAILED", since);
    }

    public static LocalDateTime startOfToday() {
        return LocalDate.now().atStartOfDay();
    }

    public static LocalDateTime endOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }
}
