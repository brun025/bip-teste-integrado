package com.example.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Benefícios")
                        .version("1.0.0")
                        .description("API REST para gerenciamento de benefícios com suporte a transferências seguras")
                        .contact(new Contact()
                                .name("Equipe de Desenvolvimento")
                                .email("brunofelipedk@gmail.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}