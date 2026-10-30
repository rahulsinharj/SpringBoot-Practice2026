package com.enterprise.mongodb.config;

import com.enterprise.mongodb.config.converter.MoneyReadConverter;
import com.enterprise.mongodb.config.converter.MoneyWriteConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.List;

/**
 * Enterprise MongoDB Configuration:
 * - Distributed Multi-Document Transaction Management
 * - Explicit Custom Conversions for Domain Value Objects
 * - Automated Auditing via AuditorAware
 */
@Configuration
@EnableTransactionManagement
@EnableMongoAuditing(auditorAwareRef = "auditorAware")
@EnableMongoRepositories(basePackages = "com.enterprise.mongodb.repository")
public class MongoConfig {

    /**
     * Configures the transaction manager required for multi-document ACID transactions.
     * Note: Requires MongoDB to operate as a replica set or sharded cluster.
     */
    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }

    /**
     * Registers custom BSON converters for domain value types like Money.
     */
    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(
                new MoneyWriteConverter(),
                new MoneyReadConverter()
        ));
    }
}
