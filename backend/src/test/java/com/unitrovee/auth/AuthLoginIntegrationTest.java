package com.unitrovee.auth;

import com.unitrovee.AbstractIntegrationTest;
import com.unitrovee.school.SchoolRepository;
import com.unitrovee.school.domain.School;
import com.unitrovee.user.UserRepository;
import com.unitrovee.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unitrovee.security.JwtService;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.assertFalse;

@AutoConfigureMockMvc
@Transactional
public class AuthLoginIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    SchoolRepository schoolRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    JwtService jwtService;
    @Autowired
    EntityManager entityManager;


    @Test
    void login_returnsAccessTokenForValidCredentials() throws Exception {
        String email = "login-student@ucdconnect.ie";
        String password = "rainy day cedar";

        School school = schoolRepository.findByEmailDomain("ucdconnect.ie")
                .orElseThrow();

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName("Login Student");
        user.setSchool(school);
        userRepository.saveAndFlush(user);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email": "%s",
                            "password": "%s"
                        }
                        """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    void login_issuesTokenWithEmailSubjectAndStudentRole() throws Exception {
        String email = "claims-student@ucdconnect.ie";
        String password = "rainy day cedar";

        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName("Claims Student");
        user.setSchool(school);
        userRepository.saveAndFlush(user);

        String responseBody = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email": "%s",
                            "password": "%s"
                        }
                        """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = objectMapper.readTree(responseBody)
                .path("data")
                .path("accessToken")
                .asText();

        assertEquals(email, jwtService.extractUsername(accessToken));
        assertEquals("ROLE_STUDENT", jwtService.extractClaim(accessToken, claims -> claims.get("role", String.class)));
    }

    @Test
    void login_rejectsWrongPassword() throws Exception {
        String email = "wrong-password@ucdconnect.ie";
        String correctPassword = "rainy day cedar";
        String wrongPassword = "wrong password";

        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(correctPassword));
        user.setDisplayName("Wrong Password Student");
        user.setSchool(school);
        userRepository.saveAndFlush(user);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email": "%s",
                            "password": "%s"
                        }
                        """.formatted(email, wrongPassword)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.error.message").value("Invalid email or password"));
    }

    @Test
    void login_rejectsUnknownEmailWithGenericCredentialsError() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "nobody@ucdconnect.ie",
                              "password": "rainy day cedar"
                            }
                            """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.error.message").value("Invalid email or password"));
    }

    @Test
    void login_rejectsInvalidRequestBody() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "not-an-email",
                              "password": "rainy day cedar"
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void login_doesNotModifyUserState() throws Exception {
        String email = "read-only-login@ucdconnect.ie";
        String password = "rainy day cedar";
        String passwordHash = passwordEncoder.encode(password);
        int reputationScore = 42;

        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setDisplayName("Read Only Student");
        user.setSchool(school);
        user.setEmailVerified(false);
        user.setReputationScore(reputationScore);
        userRepository.saveAndFlush(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "%s",
                              "password": "%s"
                            }
                            """.formatted(email, password)))
                .andExpect(status().isOk());

        // clear JPA's first-level cache, then read the actual database state again
        entityManager.clear();

        User reloaderUser = userRepository.findByEmail(email).orElseThrow();

        assertFalse(reloaderUser.isEmailVerified());
        assertEquals(reputationScore, reloaderUser.getReputationScore());
        assertEquals(passwordHash, reloaderUser.getPasswordHash());
    }
}