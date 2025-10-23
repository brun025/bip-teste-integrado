package com.example.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
@ComponentScan(basePackages = {
        "com.example.backend",
        "com.example.ejb"
})
@EntityScan(basePackages = {
        "com.example.backend.entity",
        "com.example.ejb"
})
@EnableJpaRepositories(basePackages = "com.example.backend.repository")
public class BackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}