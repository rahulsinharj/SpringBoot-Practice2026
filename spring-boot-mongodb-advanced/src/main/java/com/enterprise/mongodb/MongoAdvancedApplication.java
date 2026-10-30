package com.enterprise.mongodb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enterprise Spring Boot MongoDB Reference Application.
 */
@EnableAsync
@EnableRetry
@SpringBootApplication
public class MongoAdvancedApplication {

    public static void main(String[] args) {
        SpringApplication.run(MongoAdvancedApplication.class, args);
    }
}
