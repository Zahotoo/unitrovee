package com.unitrovee.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * demonstrates the validation pipeline end-to-end:
 * @Valid on a @RequestBody triggers Bean Validation -> a failure throws
 * MethodArgumentNotValidException -> GlobalExceptionHandler turns it into a 400 + VALIDATION_ERROR envelope.
 */
@WebMvcTest(controllers = ValidationStrategyTest.DemoController.class)
@Import(ValidationStrategyTest.DemoController.class)
public class ValidationStrategyTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void invalidBody_returns400WithValidationError() throws Exception {
        // displayName is blank and email is malformed -> validation must fail
        String badJson = """
                { "displayName": "", "email": "not-an-email" }
                """;

        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void validBody_passesValidation() throws Exception {
        String goodJson = """
                { "displayName": "Alice", "email": "alice@ucd.ie" }
                """;

        mockMvc.perform(post("/test/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(goodJson))
                .andExpect(status().isOk());
    }


    // a request DTO with validation constraints declared on its fields
    record DemoRequest(
            @NotBlank @Size(min = 2, max = 50) String displayName,      // not blank, 2...50 chars
            @NotBlank @Email String email                               // not blank + well-formed email
    ) {}

    // throwaway controller: @Valid runs Bean Validation BEFORE the method body
    @RestController
    static class DemoController {
        @PostMapping("/test/validate")
        String validate(@Valid @RequestBody DemoRequest request) {
            return "ok";
        }
    }
}
