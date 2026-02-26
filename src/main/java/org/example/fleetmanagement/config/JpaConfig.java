package org.example.fleetmanagement.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "org.example.fleetmanagement.repository")
@EntityScan(basePackages = "org.example.fleetmanagement.model")
public class JpaConfig {
}
