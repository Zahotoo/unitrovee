package com.unitrovee.category;

import com.unitrovee.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

@AutoConfigureMockMvc
public class CategoryControllerTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void listCategories_returnsAllSeededCategories() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.length()").value(9))
                .andExpect(jsonPath("$.data[0].slug").value("bikes"));
    }

    @Test
    void listCategories_excludesInactiveCategories() throws Exception {
        jdbcTemplate.update(
                "UPDATE categories SET active = FALSE WHERE slug = ?",
                "sports"
        );

        try {
            mockMvc.perform(get("/api/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(8))
                    .andExpect(jsonPath("$.data[*].slug", not(hasItem("sports"))));
        } finally {
            // restore shared Testcontainers state so this test cannot affect another test
            jdbcTemplate.update(
                    "UPDATE categories SET active = TRUE WHERE slug = ?",
                    "sports"
            );
        }
    }
}
