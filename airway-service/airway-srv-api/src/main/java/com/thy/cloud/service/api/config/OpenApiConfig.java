package com.thy.cloud.service.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI configuration with JWT Bearer token authentication.
 * Access via: http://localhost:PORT/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI airwayOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Intermodal Journey Planning API")
                        .description("Multi-modal journey search, transport management, and policy engine API")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("THY Cloud Team")
                                .email("cloud@thy.com"))
                        .license(new License()
                                .name("Internal Use Only")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME,
                                new SecurityScheme()
                                        .name(BEARER_SCHEME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token (without 'Bearer ' prefix)")
                        ));
    }
}
