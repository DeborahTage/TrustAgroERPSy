package com.trustagro.config;

import com.trustagro.security.RolePermissionCatalog;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.RoleEntity;
import com.trustagro.user.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Idempotent role/permission seeder. Canonical matrix lives in {@link RolePermissionCatalog}.
 */
@Component
@Order(1)
public class RoleSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RoleSeeder.class);
    private final RoleRepository roleRepository;

    public RoleSeeder(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        log.info("Seeding canonical role permissions...");
        for (Role role : RolePermissionCatalog.canonicalRoles()) {
            List<String> permissions = RolePermissionCatalog.permissionsFor(role);
            RoleEntity entity = roleRepository.findByName(role.name())
                    .orElseGet(() -> {
                        RoleEntity r = new RoleEntity();
                        r.setName(role.name());
                        return r;
                    });
            entity.setPermissions(permissions);
            roleRepository.save(entity);
            log.info("Seeded role: {} ({} permissions)", role.name(), permissions.size());
        }
        // Keep legacy RoleEntity rows in sync for any pre-migration auth lookups by old name
        for (Role legacy : com.trustagro.security.RoleCompatibility.LEGACY_ALIASES) {
            List<String> permissions = RolePermissionCatalog.permissionsFor(legacy);
            RoleEntity entity = roleRepository.findByName(legacy.name())
                    .orElseGet(() -> {
                        RoleEntity r = new RoleEntity();
                        r.setName(legacy.name());
                        return r;
                    });
            entity.setPermissions(permissions);
            roleRepository.save(entity);
        }
    }
}
