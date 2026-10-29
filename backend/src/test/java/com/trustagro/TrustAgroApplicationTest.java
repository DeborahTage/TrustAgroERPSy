package com.trustagro;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TrustAgroApplicationTest {

    @Test
    void contextLoads() {
        // Verifies that the Spring Boot application context boots successfully,
        // all beans and dependencies wire properly, and JPA entity definitions are valid.
    }
}
