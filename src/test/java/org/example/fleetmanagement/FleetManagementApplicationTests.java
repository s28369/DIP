package org.example.fleetmanagement;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Testy jednostkowe dla kontekstu Spring (serwisy, repozytoria).
 * Używa TestConfig bez kontrolerów JavaFX, aby testy działały bez uruchomionego JavaFX.
 */
@SpringBootTest(classes = TestConfig.class)
class FleetManagementApplicationTests {

    @Test
    void contextLoads() {
    }
}
