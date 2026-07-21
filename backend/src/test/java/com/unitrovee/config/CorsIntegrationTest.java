package com.unitrovee.config;

import com.unitrovee.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class CorsIntegrationTest extends AbstractIntegrationTest {

    private static final String FRONTEND_ORIGIN = "http://localhost:5173";

    @Autowired MockMvc mockMvc;

    @Test
    void preflightFromAllowedFrontendOriginIsAllowed() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                .header("Origin", FRONTEND_ORIGIN)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Authorization, Content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", FRONTEND_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")));
    }

    @Test
    void preflightFromUnapprovedOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                .header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
