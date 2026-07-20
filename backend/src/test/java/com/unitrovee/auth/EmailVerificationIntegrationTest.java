package com.unitrovee.auth;

import com.unitrovee.AbstractIntegrationTest;
import com.unitrovee.auth.verification.EmailVerificationCode;
import com.unitrovee.auth.verification.EmailVerificationCodeRepository;
import com.unitrovee.auth.verification.VerificationCodeHasher;
import com.unitrovee.school.SchoolRepository;
import com.unitrovee.school.domain.School;
import com.unitrovee.user.UserRepository;
import com.unitrovee.user.domain.Role;
import com.unitrovee.user.domain.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
public class EmailVerificationIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired SchoolRepository schoolRepository;
    @Autowired EmailVerificationCodeRepository verificationCodeRepository;
    @Autowired EntityManager entityManager;

    @Test
    void verify_rejectsUnknownCode() throws Exception {
        mockMvc.perform(post("/api/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "missing@ucdconnect.ie",
                          "code": "123456"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_VERIFICATION_CODE"));
    }

    @Test
    void verify_marksUserVerifiedAndDeletesCode() throws Exception {
        String email = "verify-student@ucdconnect.ie";
        String plainCode = "123456";

        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("$2a$dummy");
        user.setDisplayName("Verify Student");
        user.setSchool(school);
        user.setRole(Role.STUDENT);
        user.setEmailVerified(false);
        user.setReputationScore(0);
        userRepository.saveAndFlush(user);

        EmailVerificationCode verificationCode = new EmailVerificationCode();
        verificationCode.setUser(user);
        verificationCode.setCodeHash(VerificationCodeHasher.hash(plainCode));
        verificationCode.setExpiresAt(Instant.now().plus(Duration.ofMinutes(15)));
        verificationCodeRepository.saveAndFlush(verificationCode);

        mockMvc.perform(post("/api/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "%s",
                          "code": "%s"
                        }
                        """.formatted(email, plainCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.emailVerified").value(true));

        entityManager.flush();
        entityManager.clear();

        User verifiedUser = userRepository.findByEmail(email).orElseThrow();

        assertThat(verifiedUser.isEmailVerified()).isTrue();
        assertThat(verificationCodeRepository.findByUserId(user.getId())).isEmpty();
    }

    @Test
    void verify_rejectsWrongCodeAndKeepsUserUnverified() throws Exception {
        String email = "wrong-code@ucdconnect.ie";
        String correctCode = "123456";

        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("$2a$dummy");
        user.setDisplayName("Wrong Code Student");
        user.setSchool(school);
        user.setRole(Role.STUDENT);
        user.setEmailVerified(false);
        user.setReputationScore(0);
        userRepository.saveAndFlush(user);

        EmailVerificationCode verificationCode = new EmailVerificationCode();
        verificationCode.setUser(user);
        verificationCode.setCodeHash(VerificationCodeHasher.hash(correctCode));
        verificationCode.setExpiresAt(Instant.now().plus(Duration.ofMinutes(15)));
        verificationCodeRepository.saveAndFlush(verificationCode);

        mockMvc.perform(post("/api/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "%s",
                          "code": "654321"
                        }
                        """.formatted(email)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_VERIFICATION_CODE"));

        entityManager.flush();
        entityManager.clear();

        User unchangedUser = userRepository.findByEmail(email).orElseThrow();

        assertThat(unchangedUser.isEmailVerified()).isFalse();
        assertThat(verificationCodeRepository.findByUserId(user.getId())).isPresent();
    }

    @Test
    void verify_rejectsExpiredCodeAndKeepsUserUnverified() throws Exception {
        String email = "expired-code@ucdconnect.ie";
        String code = "123456";

        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("$2a$dummy");
        user.setDisplayName("Expired Code Student");
        user.setSchool(school);
        user.setRole(Role.STUDENT);
        user.setEmailVerified(false);
        user.setReputationScore(0);
        userRepository.saveAndFlush(user);

        EmailVerificationCode verificationCode = new EmailVerificationCode();
        verificationCode.setUser(user);
        verificationCode.setCodeHash(VerificationCodeHasher.hash(code));
        verificationCode.setExpiresAt(Instant.now().minus(Duration.ofMinutes(1)));
        verificationCodeRepository.saveAndFlush(verificationCode);

        mockMvc.perform(post("/api/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "%s",
                          "code": "%s"
                        }
                        """.formatted(email, code)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_VERIFICATION_CODE"));

        entityManager.flush();
        entityManager.clear();

        User unchangedUser = userRepository.findByEmail(email).orElseThrow();

        assertThat(unchangedUser.isEmailVerified()).isFalse();
        assertThat(verificationCodeRepository.findByUserId(user.getId())).isPresent();
    }
}