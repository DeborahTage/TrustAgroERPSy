package com.trustagro.config;

import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.entity.UserStatus;
import com.trustagro.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InitialAdminProvisionerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        lenient().when(passwordEncoder.encode(any())).thenAnswer(invocation -> "encoded_" + invocation.getArgument(0));
    }

    @Test
    void skipsProvisioningWhenAdminAlreadyExists() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(true);

        InitialAdminProvisioner provisioner = new InitialAdminProvisioner(
                userRepository, passwordEncoder, "admin@example.com", "SecurePassword123!", "Admin", "+251911223344"
        );

        provisioner.run();

        verify(userRepository, never()).save(any());
    }

    @Test
    void provisionsAdminWhenNoAdminExistsAndCredentialsProvided() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);

        InitialAdminProvisioner provisioner = new InitialAdminProvisioner(
                userRepository, passwordEncoder, "admin@example.com", "SecurePassword123!", "Admin User", "+251911223344"
        );

        provisioner.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("admin@example.com");
        assertThat(saved.getFullName()).isEqualTo("Admin User");
        assertThat(saved.getRole()).isEqualTo(Role.ADMIN);
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.getPassword()).isEqualTo("encoded_SecurePassword123!");
    }

    @Test
    void skipsProvisioningWhenNoCredentialsProvided() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);

        InitialAdminProvisioner provisioner = new InitialAdminProvisioner(
                userRepository, passwordEncoder, "", "", "Admin User", "+251911223344"
        );

        provisioner.run();

        verify(userRepository, never()).save(any());
    }

    @Test
    void failsWhenPasswordIsShorterThan8Chars() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);

        InitialAdminProvisioner provisioner = new InitialAdminProvisioner(
                userRepository, passwordEncoder, "admin@example.com", "short", "Admin User", "+251911223344"
        );

        assertThatThrownBy(provisioner::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("INITIAL_ADMIN_PASSWORD must be at least 8 characters");

        verify(userRepository, never()).save(any());
    }
}
