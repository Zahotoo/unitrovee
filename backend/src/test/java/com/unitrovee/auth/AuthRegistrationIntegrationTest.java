package com.unitrovee.auth;

import com.unitrovee.AbstractIntegrationTest;
import com.unitrovee.school.SchoolRepository;
import com.unitrovee.school.domain.School;
import com.unitrovee.user.UserRepository;
import com.unitrovee.user.domain.Role;
import com.unitrovee.user.domain.User;
import com.unitrovee.auth.verification.EmailVerificationCode;
import com.unitrovee.auth.verification.EmailVerificationCodeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

@AutoConfigureMockMvc
@Transactional
class AuthRegistrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc         mockMvc;
    @Autowired
    UserRepository  userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    SchoolRepository schoolRepository;
    @Autowired
    EmailVerificationCodeRepository verificationCodeRepository;

    @Test
    void register_createsUnverifiedStudentForMatchingSchool() throws Exception {
        String email = "new-student@ucdconnect.ie";
        String password = "rainy day cedar";

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email": "%s",
                            "password": "%s",
                            "displayName": "New Student"
                        }
                        """.formatted(email, password)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.displayName").value("New Student"))
                .andExpect(jsonPath("$.data.emailVerified").value(false))
                .andExpect(jsonPath("$.data.school.shortName").value("UCD"));

        User savedUser = userRepository.findByEmail(email).orElseThrow();

        EmailVerificationCode verificationCode = verificationCodeRepository.findByUserId(savedUser.getId()).orElseThrow();

        assertThat(savedUser.getPasswordHash()).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, savedUser.getPasswordHash())).isTrue();
        assertThat(savedUser.getRole()).isEqualTo(Role.STUDENT);
        assertThat(savedUser.isEmailVerified()).isFalse();
        assertThat(savedUser.getReputationScore()).isZero();
        assertThat(savedUser.getSchool().getEmailDomain()).isEqualTo("ucdconnect.ie");
        assertThat(verificationCode.getCodeHash()).hasSize(64);
        assertThat(verificationCode.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void register_rejectsEmailAlreadyUsedAfterNormalization() throws Exception {
        String firstEmail = "Duplicate@UCDCONNECT.IE";
        String secondEmail = "duplicate@ucdconnect.ie";
        String password =  "rainy day cedar";

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email": "%s",
                            "password": "%s",
                            "displayName": "First Student"
                        }
                        """.formatted(firstEmail, password)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "displayName": "Second Student"
                                }
                                """.formatted(secondEmail, password)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void register_rejectsEmailFromUnsupportedSchoolDomain() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "student@gmail.com",
                                  "password": "rainy day cedar",
                                  "displayName": "Unsupported Student"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_SCHOOL_EMAIL"));
    }

    @Test
    void register_rejectsPasswordShorterThanEightCharacters() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "student@ucdconnect.ie",
                                  "password": "short",
                                  "displayName": "Weak Password Student"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value(
                        "password: Password must be between 8 and 72 characters"
                ));
    }

    @Test
    void register_rejectsInvalidEmailFormat() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "email": "not-an-email",
                      "password": "rainy day cedar",
                      "displayName": "Invalid Email Student"
                    }
                    """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("email: Email must be valid"));
    }

    @Test
    void register_rejectsEmailFromInactiveSchool() throws Exception {
        School school = schoolRepository.findByEmailDomain("ucdconnect.ie")
                .orElseThrow();

        school.setActive(false);
        schoolRepository.saveAndFlush(school);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "email": "inactive-student@ucdconnect.ie",
                      "password": "rainy day cedar",
                      "displayName": "Inactive School Student"
                    }
                    """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_SCHOOL_EMAIL"));
    }
}
