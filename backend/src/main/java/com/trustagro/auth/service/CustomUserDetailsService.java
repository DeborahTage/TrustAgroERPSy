package com.trustagro.auth.service;

import com.trustagro.user.entity.User;
import com.trustagro.user.entity.UserStatus;
import com.trustagro.user.repository.UserRepository;
import com.trustagro.security.RoleCompatibility;
import com.trustagro.security.RolePermissionCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        var canonical = RoleCompatibility.normalize(user.getRole());
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + canonical.name()));
        // Also expose stored role if still legacy so hasRole checks during transition don't break
        if (user.getRole() != null && user.getRole() != canonical) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        }
        for (String permission : RolePermissionCatalog.permissionsFor(canonical)) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }

        boolean enabled = user.getStatus() != UserStatus.INACTIVE;
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                enabled,
                true,
                true,
                true,
                authorities
        );
    }
}
