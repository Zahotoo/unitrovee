package com.unitrovee.user;

import com.unitrovee.AbstractIntegrationTest;
import com.unitrovee.school.SchoolRepository;
import com.unitrovee.school.domain.School;
import com.unitrovee.security.CustomUserDetailsService;
import com.unitrovee.security.JwtService;
import com.unitrovee.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
public class CurrentUserIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired SchoolRepository schoolRepository;
    @Autowired CustomUserDetailsService userDetailsService;
    @Autowired JwtService jwtService;

    @Test
    void currentUser_returnsProfileForAuthenticatedUser() throws Exception {
        String email = "me-student@ucdconnect.ie";
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("$2a$dummy");
        user.setDisplayName("Current Student");
        user.setSchool(school);
        userRepository.saveAndFlush(user);

        String accessToken = jwtService.generateToken(userDetailsService.loadUserByUsername(email));

        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(user.getId()))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.displayName").value("Current Student"))
                .andExpect(jsonPath("$.data.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.emailVerified").value(false))
                .andExpect(jsonPath("$.data.school.id").value(school.getId()))
                .andExpect(jsonPath("$.data.school.name").value(school.getName()))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.reputationScore").value(0));
    }

    @Test
    void currentUser_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.error.message").value("Authentication required"));
    }

    @Test
    void currentUser_returnsOnlyTheUserIdentifiedByTheToken() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();

        User firstUser = new User();
        firstUser.setEmail("first-student@ucdconnect.ie");
        firstUser.setPasswordHash("$2a$first");
        firstUser.setDisplayName("First Student");
        firstUser.setSchool(school);
        userRepository.saveAndFlush(firstUser);

        User secondUser = new User();
        secondUser.setEmail("second-student@ucdconnect.ie");
        secondUser.setPasswordHash("$2a$second");
        secondUser.setDisplayName("Second Student");
        secondUser.setSchool(school);
        userRepository.saveAndFlush(secondUser);

        String accessToken = jwtService.generateToken(userDetailsService.loadUserByUsername(firstUser.getEmail()));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(firstUser.getId()))
                .andExpect(jsonPath("$.data.email").value("first-student@ucdconnect.ie"))
                .andExpect(jsonPath("$.data.displayName").value("First Student"));
    }
}
