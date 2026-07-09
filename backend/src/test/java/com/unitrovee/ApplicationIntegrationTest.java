package com.unitrovee;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ApplicationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;      // simple sql runner, auto-configured from the datasource

    @Test
    void contextLoads() {
        // reaching here means the whole Spring context started against the real DB.
    }

    @Test
    void flyway_applicationBaselineMigration() {
        // flyway's history table should exist and contain V1 (baseline) as a successful migration
        Long count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success = true",
                Long.class
        );
        assertThat(count).isEqualTo(1L);
    }
}
