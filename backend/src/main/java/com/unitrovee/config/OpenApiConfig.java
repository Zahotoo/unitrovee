package com.unitrovee.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Global OpenAPII metadata + a JWT bearer security scheme placeholder
 * springdoc picks up this OpenAPI bean and builds the Swagger UI from it
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI unitroveeOpenAPI() {
        return new OpenAPI()
                // API metadata shown at the top of the Swagger UI page
                .info(new Info()
                        .title("Unitrovee API")
                        .version("v1")
                        .description("Second-hand marketplace for verified Irish students."))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
