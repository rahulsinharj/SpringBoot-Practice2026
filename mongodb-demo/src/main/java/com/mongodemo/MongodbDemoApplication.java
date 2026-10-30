package com.mongodemo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;

@SpringBootApplication
public class MongodbDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MongodbDemoApplication.class, args);
        System.out.println("MongoDB Demo Application is running...");
    }

    @Bean
    public CommandLineRunner verifyMongoDb(MongoTemplate mongoTemplate) {
        return args -> {
            System.out.println("==========================================");
            System.out.println("CONNECTED TO MONGODB DATABASE: " + mongoTemplate.getDb().getName());
            System.out.println("==========================================");
        };
    }

}
