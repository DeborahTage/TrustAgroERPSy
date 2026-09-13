package com.trustagro.config;

import com.trustagro.user.entity.RoleEntity;
import com.trustagro.user.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RoleSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RoleSeeder.class);
    private final RoleRepository roleRepository;

    public RoleSeeder(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    private static final Map<String, List<String>> ROLE_PERMISSIONS = Map.ofEntries(
        Map.entry("ADMIN", List.of("*")),
        Map.entry("FARM_MANAGER", List.of(
            "farms:read","farms:write","flocks:read","flocks:write",
            "daily_records:read","daily_records:write","chicken_sales:read","chicken_sales:write",
            "reports:read","notifications:read","settings:read"
        )),
        Map.entry("INVENTORY_MANAGER", List.of(
            "inventory:read","inventory:write","stock_in:write","stock_out:write",
            "suppliers:read","suppliers:write","reports:read","notifications:read","settings:read"
        )),
        Map.entry("PHARMACIST", List.of(
            "pharmacy_sales:read","pharmacy_sales:write","inventory:read",
            "notifications:read","settings:read"
        )),
        Map.entry("VETERINARIAN", List.of(
            "flocks:read","veterinary:read","veterinary:write",
            "disease_cases:read","disease_cases:write","vaccinations:read","vaccinations:write",
            "prescriptions:read","prescriptions:write","treatments:read","treatments:write",
            "reports:read","notifications:read","settings:read"
        )),
        Map.entry("FINANCE", List.of(
            "finance:read","finance:write","revenue:read","revenue:write",
            "costs:read","costs:write","invoices:read","invoices:write",
            "reports:read","reports:export","notifications:read","settings:read"
        )),
        Map.entry("CRM_MANAGER", List.of(
            "crm:read","crm:write","leads:read","leads:write",
            "pipeline:read","pipeline:write","orders:read","orders:write",
            "consultancy:read","consultancy:write","training:read",
            "notifications:read","settings:read"
        )),
        Map.entry("MARKETING", List.of(
            "marketing:read","marketing:write","campaigns:read","campaigns:write",
            "leads:read","leads:write","reports:read","notifications:read","settings:read"
        )),
        Map.entry("TRAINING_OFFICER", List.of(
            "training:read","training:write","notifications:read","settings:read"
        ))
    );

    @Override
    public void run(String... args) {
        log.info("Seeding role permissions...");
        ROLE_PERMISSIONS.forEach((roleName, permissions) -> {
            RoleEntity role = roleRepository.findByName(roleName)
                    .orElseGet(() -> {
                        RoleEntity r = new RoleEntity();
                        r.setName(roleName);
                        return r;
                    });
            role.setPermissions(permissions);
            roleRepository.save(role);
            log.info("Seeded role: {} ({} permissions)", roleName, permissions.size());
        });
    }
}
