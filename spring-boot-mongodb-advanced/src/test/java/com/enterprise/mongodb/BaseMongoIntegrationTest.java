package com.enterprise.mongodb;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base integration test configuring real MongoDB Replica Set container via Testcontainers.
 * Provides true production parity for multi-document ACID transactions and change streams.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseMongoIntegrationTest {

    @Container
    protected static final MongoDBContainer MONGO_CONTAINER = new MongoDBContainer("mongo:7.0.5");

    @DynamicPropertySource
    static void configureMongoProperties(DynamicPropertyRegistry registry) {
        // Supplies the dynamic replica set URL from the running Docker container
        registry.add("spring.data.mongodb.uri", MONGO_CONTAINER::getReplicaSetUrl);
    }
}
