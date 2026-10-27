package com.trustagro.config;

import com.trustagro.security.RolePermissionCatalog;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.RoleEntity;
import com.trustagro.user.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests verifying that {@link RoleSeeder}:
 * 1. Faithfully enforces the canonical code catalog {@link RolePermissionCatalog}.
 * 2. Idempotently restores canonical permissions when database rows are modified.
 * 3. Preserves existing role entities without creating duplicates or deleting records.
 */
@ExtendWith(MockitoExtension.class)
class RoleSeederTest {

    @Mock
    private RoleRepository roleRepository;

    private final Map<String, RoleEntity> database = new HashMap<>();

    @BeforeEach
    void setUp() {
        lenient().when(roleRepository.findByName(any())).thenAnswer(inv ->
                Optional.ofNullable(database.get(inv.getArgument(0))));
        lenient().when(roleRepository.save(any(RoleEntity.class))).thenAnswer(inv -> {
            RoleEntity entity = inv.getArgument(0);
            database.put(entity.getName(), entity);
            return entity;
        });
    }

    @Test
    void initialSeedCreatesAllCanonicalRoles() {
        RoleSeeder seeder = new RoleSeeder(roleRepository);
        seeder.run();

        for (Role role : RolePermissionCatalog.canonicalRoles()) {
            RoleEntity entity = database.get(role.name());
            assertNotNull(entity, "Role must be seeded: " + role.name());
            assertEquals(RolePermissionCatalog.permissionsFor(role), entity.getPermissions());
        }
    }

    @Test
    void repeatedStartupIsIdempotentAndRestoresCanonicalPermissions() {
        RoleSeeder seeder = new RoleSeeder(roleRepository);
        seeder.run();

        int initialSize = database.size();

        // Simulate an administrator manually tampering with permissions in the database
        RoleEntity adminRole = database.get(Role.ADMIN.name());
        assertNotNull(adminRole);
        adminRole.setPermissions(List.of("UNAUTHORIZED_EXTRA_PERMISSION"));

        // Second startup run
        seeder.run();

        // Ensure database size has not grown (no duplicates)
        assertEquals(initialSize, database.size());

        // Ensure the code-defined role catalog is authoritative and reconciled the tampered permissions
        RoleEntity restoredAdmin = database.get(Role.ADMIN.name());
        assertEquals(RolePermissionCatalog.permissionsFor(Role.ADMIN), restoredAdmin.getPermissions());
        assertFalse(restoredAdmin.getPermissions().contains("UNAUTHORIZED_EXTRA_PERMISSION"));
    }
}
