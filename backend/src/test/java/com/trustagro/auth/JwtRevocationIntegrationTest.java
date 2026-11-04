package com.trustagro.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.entity.UserStatus;
import com.trustagro.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtRevocationIntegrationTest {

    private static final String TEST_PASSWORD = "test-password-123";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setFullName("JWT Test User");
        user.setEmail("jwt-" + UUID.randomUUID() + "@trustagro.test");
        user.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        user.setRole(Role.PHARMACIST);
        user.setStatus(UserStatus.ACTIVE);
        user.setTokenVersion(0L);
        user = userRepository.saveAndFlush(user);
    }

    @Test
    void logoutRevokesTheExistingBearerToken() throws Exception {
        String token = login(TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordChangeRevokesOldTokenAndAllowsNewPasswordLogin() throws Exception {
        String token = login(TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"test-password-123","newPassword":"new-test-password-456"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        String newToken = login("new-test-password-456");
        assertNotNull(newToken);
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk());
    }

    @Test
    void disablingUserRejectsPreviouslyIssuedToken() throws Exception {
        String token = login(TEST_PASSWORD);
        user.setStatus(UserStatus.INACTIVE);
        userRepository.saveAndFlush(user);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletedUserWithOtherwiseValidTokenIsRejectedWithUnauthorized() throws Exception {
        String token = login(TEST_PASSWORD);
        userRepository.delete(user);
        userRepository.flush();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordChangeIncrementsTokenVersionExactlyOnceAndMaintainsConsistentEntityState() throws Exception {
        Long initialVersion = user.getTokenVersion();
        String oldToken = login(TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + oldToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"test-password-123","newPassword":"new-test-password-456"}
                                """))
                .andExpect(status().isOk());

        User updated = userRepository.findById(user.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(initialVersion + 1L, updated.getTokenVersion());
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("new-test-password-456", updated.getPassword()));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());

        String newToken = login("new-test-password-456");
        assertNotNull(newToken);
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk());
    }

    private String login(String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("email", user.getEmail(), "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode token = objectMapper.readTree(response).path("data").path("token");
        assertNotNull(token.textValue());
        return token.textValue();
    }
}
