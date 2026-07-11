package com.unitrovee.school;

import com.unitrovee.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class SchoolRepositoryTest extends AbstractIntegrationTest{

    @Autowired
    SchoolRepository schoolRepository;

    @Test
    void seedData_loadsAllSchools() {
        assertThat(schoolRepository.count()).isEqualTo(13);
    }

    @Test
    void findByEmailDomain_returnsMatchingSchool() {
        // Verify the derived query works AND the UCD seed row is correct
        assertThat(schoolRepository.findByEmailDomain("ucdconnect.ie"))
                .isPresent()
                .get()
                .extracting(school -> school.getShortName())
                .isEqualTo("UCD");
    }
}
