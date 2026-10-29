package com.trustagro.config;

import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.entity.UserStatus;
import com.trustagro.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Idempotent, secure initial administrator provisioner.
 * <p>
 * Ensures an active administrator account exists when deploying into clean
 * staging or production databases, without relying on insecure demo seeding
 * or hardcoded credentials.
 * <p>
 * If an active administrator account already exists, provisioning cleanly no-ops.
 * If no administrator account exists, initial credentials are read from
 * environment variables (INITIAL_ADMIN_EMAIL / INITIAL_ADMIN_PASSWORD).
 */
@Component
@Order(2)
public class InitialAdminProvisioner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialAdminProvisioner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String initialAdminEmail;
    private final String initialAdminPassword;
    private final String initialAdminName;
    private final String initialAdminPhone;

    public InitialAdminProvisioner(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.initial-email:${INITIAL_ADMIN_EMAIL:}}") String initialAdminEmail,
            @Value("${app.admin.initial-password:${INITIAL_ADMIN_PASSWORD:}}") String initialAdminPassword,
            @Value("${app.admin.initial-name:${INITIAL_ADMIN_NAME:System Administrator}}") String initialAdminName,
            @Value("${app.admin.initial-phone:${INITIAL_ADMIN_PHONE:+251900000000}}") String initialAdminPhone) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.initialAdminEmail = initialAdminEmail;
        this.initialAdminPassword = initialAdminPassword;
        this.initialAdminName = initialAdminName;
        this.initialAdminPhone = initialAdminPhone;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            log.info("Active administrator account already present. Initial admin provisioning skipped.");
            return;
        }

        if (initialAdminEmail == null || initialAdminEmail.isBlank()
                || initialAdminPassword == null || initialAdminPassword.isBlank()) {
            log.warn("No administrator account found in database and INITIAL_ADMIN_EMAIL / INITIAL_ADMIN_PASSWORD " +
                    "are not configured. System is locked until an administrator account is provisioned.");
            return;
        }

        if (initialAdminPassword.trim().length() < 8) {
            throw new IllegalStateException(
                    "Initial administrator provisioning failed: INITIAL_ADMIN_PASSWORD must be at least 8 characters");
        }

        String email = initialAdminEmail.trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            log.warn("User with email {} already exists but does not have ADMIN role. Skipping initial admin provisioning.", email);
            return;
        }

        User admin = new User();
        admin.setFullName(initialAdminName != null && !initialAdminName.isBlank() ? initialAdminName.trim() : "System Administrator");
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(initialAdminPassword.trim()));
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setPhone(initialAdminPhone != null && !initialAdminPhone.isBlank() ? initialAdminPhone.trim() : "+251900000000");

        userRepository.save(admin);
        log.info("Initial administrator account successfully provisioned for: {}", email);
    }
}
