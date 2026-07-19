package com.unitrovee.security;

import com.unitrovee.AbstractIntegrationTest;
import com.unitrovee.school.SchoolRepository;
import com.unitrovee.school.domain.School;
import com.unitrovee.user.UserRepository;
import com.unitrovee.user.domain.Role;
import com.unitrovee.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
@Import(RbacIntegrationTest.AdminTestController.class)
public class RbacIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired CustomUserDetailsService userDetailsService;
    @Autowired UserRepository userRepository;
    @Autowired SchoolRepository schoolRepository;

    @RestController
    static class AdminTestController {

        @GetMapping("/api/test/admin")
        @PreAuthorize("hasRole('ADMIN')")
        String adminOnly() {
            return "admin ok";
        }
    }

    @Test
    void studentToken_cannotAccessAdminEndpoint() throws Exception {
        String email = "student-rbac@ucdconnect.ie";

        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();

        User student = new User();
        student.setEmail(email);
        student.setPasswordHash("$2a$dummy");
        student.setDisplayName("RBAC Student");
        student.setSchool(school);
        student.setRole(Role.STUDENT);
        userRepository.saveAndFlush(student);

        String accessToken = jwtService.generateToken(userDetailsService.loadUserByUsername(email));

        mockMvc.perform(get("/api/test/admin")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.message").value("Access denied"));
    }

    @Test
    void adminToken_canAccessAdminEndpoint() throws Exception {
        String email = "admin-rbac@ucdconnect.ie";
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();

        User admin = new User();
        admin.setEmail(email);
        admin.setPasswordHash("$2a$dummy");
        admin.setDisplayName("RBAC Admin");
        admin.setSchool(school);
        admin.setRole(Role.ADMIN);
        userRepository.saveAndFlush(admin);

        String accessToken = jwtService.generateToken(userDetailsService.loadUserByUsername(email));

        mockMvc.perform(get("/api/test/admin")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("admin ok"));
    }

    @Test
    void missingToken_cannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/test/admin"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.error.message").value("Authentication required"));
    }
}
