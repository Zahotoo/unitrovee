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

    @Test
    void resend_createsANewCodeForAnUnverifiedUserWithoutAnActiveCode() throws Exception {
        String email = "resend-student@ucdconnect.ie";

        School school = schoolRepository.findByEmailDomain("ucdconnect.ie").orElseThrow();

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("$2a$dummy");
        user.setDisplayName("Resend Student");
        user.setSchool(school);
        user.setRole(Role.STUDENT);
        user.setEmailVerified(false);
        user.setReputationScore(0);
        userRepository.saveAndFlush(user);

        Instant requestedAt = Instant.now();

        mockMvc.perform(post("/api/auth/resend-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s"
                    }
                    """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.retryAfterSeconds").value(60));

        EmailVerificationCode verificationCode = verificationCodeRepository.findByUserId(user.getId()).orElseThrow();

        assertThat(verificationCode.getCodeHash()).hasSize(64);
        assertThat(verificationCode.getExpiresAt()).isAfter(requestedAt.plus(Duration.ofMinutes(14)));
    }

    @Test
    void resend_hidesTheCooldownAndDoesNotReplaceTheCode() throws Exception {
        String email = "cooldown-student@ucdconnect.ie";

        School school = schoolRepository.findByEmailDomain("ucdconnect.ie")
                .orElseThrow();

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("$2a$dummy");
        user.setDisplayName("Cooldown Student");
        user.setSchool(school);
        user.setRole(Role.STUDENT);
        user.setEmailVerified(false);
        user.setReputationScore(0);
        userRepository.saveAndFlush(user);

        EmailVerificationCode verificationCode = new EmailVerificationCode();
        verificationCode.setUser(user);
        verificationCode.setCodeHash(
                VerificationCodeHasher.hash("123456")
        );
        verificationCode.setExpiresAt(
                Instant.now().plus(Duration.ofMinutes(15))
        );
        verificationCodeRepository.saveAndFlush(verificationCode);

        String originalHash = verificationCode.getCodeHash();

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "email": "%s"
                        }
                        """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.retryAfterSeconds").value(60))
                .andExpect(jsonPath("$.message").value(
                        "If your account is eligible, a verification code has been sent"
                ));

        entityManager.clear();

        EmailVerificationCode unchangedCode = verificationCodeRepository
                .findByUserId(user.getId())
                .orElseThrow();

        assertThat(unchangedCode.getCodeHash()).isEqualTo(originalHash);
    }

    @Test
    void resend_replacesTheOldCodeAfterTheCooldown() throws Exception {
        String email = "replace-code-student@ucdconnect.ie";
        String oldCode = "123456";

        School school = schoolRepository.findByEmailDomain("ucdconnect.ie")
                .orElseThrow();

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("$2a$dummy");
        user.setDisplayName("Replace Code Student");
        user.setSchool(school);
        user.setRole(Role.STUDENT);
        user.setEmailVerified(false);
        user.setReputationScore(0);
        userRepository.saveAndFlush(user);

        EmailVerificationCode verificationCode = new EmailVerificationCode();
        verificationCode.setUser(user);
        verificationCode.setCodeHash(VerificationCodeHasher.hash(oldCode));
        verificationCode.setExpiresAt(Instant.now().plus(Duration.ofMinutes(15)));
        verificationCodeRepository.saveAndFlush(verificationCode);

        // Simulate that 61 seconds have passed without making the test wait.
        entityManager.createNativeQuery("""
                UPDATE email_verification_codes
                SET updated_at = (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')
                    - INTERVAL '61 seconds'
                WHERE user_id = :userId
                """)
                .setParameter("userId", user.getId())
                .executeUpdate();

        entityManager.clear();

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.retryAfterSeconds").value(60));

        EmailVerificationCode refreshedCode = verificationCodeRepository
                .findByUserId(user.getId())
                .orElseThrow();

        assertThat(refreshedCode.getCodeHash())
                .isNotEqualTo(VerificationCodeHasher.hash(oldCode));

        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "code": "%s"
                                }
                                """.formatted(email, oldCode)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_VERIFICATION_CODE"));
    }

    @Test
    void resend_returnsTheSameGenericResponseForUnknownAndVerifiedEmails()
            throws Exception {
        String unknownEmail = "unknown-resend@ucdconnect.ie";
        String verifiedEmail = "verified-resend@ucdconnect.ie";

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "email": "%s"
                        }
                        """.formatted(unknownEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.retryAfterSeconds").value(60))
                .andExpect(jsonPath("$.message").value(
                        "If your account is eligible, a verification code has been sent"
                ));

        School school = schoolRepository.findByEmailDomain("ucdconnect.ie")
                .orElseThrow();

        User verifiedUser = new User();
        verifiedUser.setEmail(verifiedEmail);
        verifiedUser.setPasswordHash("$2a$dummy");
        verifiedUser.setDisplayName("Verified Resend Student");
        verifiedUser.setSchool(school);
        verifiedUser.setRole(Role.STUDENT);
        verifiedUser.setEmailVerified(true);
        verifiedUser.setReputationScore(0);
        userRepository.saveAndFlush(verifiedUser);

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "email": "%s"
                        }
                        """.formatted(verifiedEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.retryAfterSeconds").value(60))
                .andExpect(jsonPath("$.message").value(
                        "If your account is eligible, a verification code has been sent"
                ));

        assertThat(
                verificationCodeRepository.findByUserId(verifiedUser.getId())
        ).isEmpty();
    }
}