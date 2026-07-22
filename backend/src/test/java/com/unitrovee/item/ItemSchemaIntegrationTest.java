package com.unitrovee.item;

import com.unitrovee.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;


public class ItemSchemaIntegrationTest extends AbstractIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void itemsTable_existsAfterFlywayMigration() {
        String tableName = jdbcTemplate.queryForObject(
                "SELECT to_regclass('public.items')::text",
                String.class
        );

        assertThat(tableName).isEqualTo("items");
    }
}
