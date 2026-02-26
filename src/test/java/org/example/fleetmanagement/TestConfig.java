package org.example.fleetmanagement;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

/**
 * Konfiguracja testowa – ładuje kontekst Spring bez kontrolerów JavaFX,
 * aby testy mogły działać w środowisku bez uruchomionego JavaFX Toolkit.
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan(
    basePackages = "org.example.fleetmanagement",
    useDefaultFilters = false,
    includeFilters = {
        @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Service.class),
        @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Repository.class)
    }
)
public class TestConfig {
}
