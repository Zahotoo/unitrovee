package com.unitrovee.security;

import com.unitrovee.AbstractIntegrationTest;
import com.unitrovee.school.SchoolRepository;
import com.unitrovee.school.domain.School;
import com.unitrovee.user.UserRepository;
import com.unitrovee.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
@Import(JwtAuthenticationIntegrationTest.SecuredTestController.class)
public class JwtAuthenticationIntegrationTest extends AbstractIntegrationTest{

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired CustomUserDetailsService userDetailsService;
    @Autowired UserRepository userRepository;
    @Autowired SchoolRepository schoolRepository;

    // a protected endpoint that exists ONLY for this test
    @RestController
    static class SecuredTestController {
        @GetMapping("/api/test/secured")
        String secured() { return "ok"; }
    }

    @Test
    void validToken_authenticates_returns200() throws Exception {
        String token = jwtService.generateToken(persistUserAndLoad());
        mockMvc.perform(get("/api/test/secured")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void missingToken_returns401() throws Exception {
        mockMvc.perform(get("/api/test/secured"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/test/secured")
                .header("Authorization", "Bearer not.a.real.token"))
                .andExpect(status().isUnauthorized());
    }

    // persist a user (the filter reloads users from the DB) and return it as UserDetails
    private UserDetails persistUserAndLoad() throws Exception {
        School school = schoolRepository.findAll().get(0);
        User user = new User();
        user.setEmail("jwt-test@ucdconnect.ie");
        user.setPasswordHash("$2a$dummy");
        user.setDisplayName("Jwt Test");
        user.setSchool(school);
        userRepository.saveAndFlush(user);
        return userDetailsService.loadUserByUsername("jwt-test@ucdconnect.ie");
    }
}
