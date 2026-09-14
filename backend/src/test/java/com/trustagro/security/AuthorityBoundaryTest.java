package com.trustagro.security;

import com.trustagro.user.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lightweight authority checks simulating Spring method-security decisions.
 */
class AuthorityBoundaryTest {

    private boolean hasAuthority(Authentication auth, String authority) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(authority));
    }

    private Authentication authWith(String... authorities) {
        return new UsernamePasswordAuthenticationToken(
                "user", "n/a",
                java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()
        );
    }

    private static String[] roleAnd(List<String> perms, String role) {
        String[] out = new String[perms.size() + 1];
        out[0] = role;
        for (int i = 0; i < perms.size(); i++) out[i + 1] = perms.get(i);
        return out;
    }

    @Test
    void pharmacistAuthoritiesAllowSaleButDenyStockIn() {
        List<String> perms = RolePermissionCatalog.permissionsFor(Role.PHARMACIST);
        Authentication auth = authWith(roleAnd(perms, "ROLE_PHARMACIST"));
        assertTrue(hasAuthority(auth, Permissions.PHARMACY_SALE));
        assertTrue(hasAuthority(auth, Permissions.PHARMACY_DISPENSE));
        assertFalse(hasAuthority(auth, Permissions.INVENTORY_STOCK_IN));
        assertFalse(hasAuthority(auth, Permissions.INVENTORY_TRANSFER));
    }

    @Test
    void unauthorizedUserLacksDomainAuthorities() {
        Authentication auth = authWith("ROLE_USER");
        assertFalse(hasAuthority(auth, Permissions.FINANCE_READ));
        assertFalse(hasAuthority(auth, Permissions.CRM_READ));
    }

    @Test
    void financeCannotPerformVeterinaryActions() {
        List<String> perms = RolePermissionCatalog.permissionsFor(Role.FINANCE);
        Authentication auth = authWith(roleAnd(perms, "ROLE_FINANCE"));
        assertTrue(hasAuthority(auth, Permissions.FINANCE_CREATE));
        assertFalse(hasAuthority(auth, Permissions.VETERINARY_PRESCRIBE));
        assertFalse(hasAuthority(auth, Permissions.VETERINARY_CREATE));
    }
}
