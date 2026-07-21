package com.unitrovee.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unitrovee.AbstractIntegrationTest;
import com.unitrovee.auth.verification.VerificationEmailSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
@Import(AuthWorkflowIntegrationTest.TestEmailConfiguration.class)
public class AuthWorkflowIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CapturingVerificationEmailSender verificationEmailSender;

    @Test
    void registerVerifyLoginAndRealCurrentUser() throws Exception {
        String email = "workflow-student@ucdconnect.ie";
        String password = "rainy day cedar";
        String displayName = "Workflow Student";

        // 1. Register creates an unverified student and sends an OTP
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "%s",
                          "password": "%s",
                          "displayName": "%s"
                        }
                        """.formatted(email, password, displayName)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.emailVerified").value(false));

        // Test-only sender receives the plain OTP exactly as an email provider would
        String otp = verificationEmailSender.codeFor(email);

        // 2. OTP verification marks the account as verified
        mockMvc.perform(post("/api/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "%s",
                          "code": "%s"
                        }
                        """.formatted(email, otp)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.emailVerified").value(true));

        // 3. Login returns a JWT access token
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = objectMapper.readTree(loginResponse)
                .path("data")
                .path("accessToken")
                .asText();

        // 4. JWT authenticates the request to the protected profile endpoint
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.displayName").value(displayName))
                .andExpect(jsonPath("$.data.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.emailVerified").value(true))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestEmailConfiguration {

        @Bean
        @Primary
        CapturingVerificationEmailSender verificationEmailSender() {
            return new CapturingVerificationEmailSender();
        }
    }

    static class CapturingVerificationEmailSender implements VerificationEmailSender {

        private final Map<String, String> codesByEmail = new ConcurrentHashMap<>();

        @Override
        public void sendVerificationCode(String email, String code) {
            codesByEmail.put(email, code);
        }

        String codeFor(String email) {
            String code = codesByEmail.get(email);

            if (code == null) {
                throw new IllegalStateException(
                        "No verification code was sent for email: "  + email
                );
            }
            return code;
        }
    }
}
