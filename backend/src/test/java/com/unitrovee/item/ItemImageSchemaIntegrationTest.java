package com.unitrovee.item;

import com.unitrovee.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

public class ItemImageSchemaIntegrationTest extends AbstractIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void itemImagesTable_existsAfterFlywayMigration() {
        String tableName = jdbcTemplate.queryForObject(
                "SELECT to_regclass('public.item_images')::text",
                String.class
        );

        assertThat(tableName).isEqualTo("item_images");
    }
}
