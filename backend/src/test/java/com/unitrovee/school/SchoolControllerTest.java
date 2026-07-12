package com.unitrovee.school;

import com.unitrovee.AbstractIntegrationTest;
import com.unitrovee.school.domain.School;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SchoolControllerTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    SchoolRepository schoolRepository;

    @Test
    void listSchools_returnsAllActiveSeeds() throws Exception {
        mockMvc.perform(get("/api/schools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.totalElements").value(13))
                .andExpect(jsonPath("$.data.content.length()").value(13));
    }

    @Test
    void listSchools_respectsPagination() throws Exception {
        mockMvc.perform(get("/api/schools").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(5))
                .andExpect(jsonPath("$.data.totalElements").value(13))
                .andExpect(jsonPath("$.data.totalPages").value(3));
    }

    @Test
    void getSchoolById_returnsThatSchool() throws Exception {
        // grab a real seeded row so we don't hardcode an id that might shift
        School school = schoolRepository.findAll().get(0);

        mockMvc.perform(get("/api/schools/{id}", school.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shortName").value(school.getShortName()))
                .andExpect(jsonPath("$.data.emailDomain").value(school.getEmailDomain()));
    }

    @Test
    void getSchoolById_missing_returns404() throws Exception {
        mockMvc.perform(get("/api/schools/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }
}
