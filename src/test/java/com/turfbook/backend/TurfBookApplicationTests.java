package com.turfbook.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class TurfBookApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring context loads without errors
    }
}
