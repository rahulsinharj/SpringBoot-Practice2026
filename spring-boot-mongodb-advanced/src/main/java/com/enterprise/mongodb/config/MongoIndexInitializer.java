package com.enterprise.mongodb.config;

import com.enterprise.mongodb.domain.model.Account;
import com.enterprise.mongodb.domain.model.Customer;
import com.enterprise.mongodb.domain.model.LedgerEntry;
import com.enterprise.mongodb.domain.model.event.EventDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Enterprise programmatic MongoDB index initializer.
 * Guarantees indexes exist at runtime in production environments where
 * 'auto-index-creation' is disabled to prevent unmanaged startup lock contention.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MongoIndexInitializer {

    private final MongoTemplate mongoTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeIndexes() {
        log.info("Ensuring enterprise MongoDB indexes programmatically...");

        ensureAccountIndexes();
        ensureLedgerIndexes();
        ensureEventIndexes();
        ensureCustomerIndexes();

        log.info("Programmatic MongoDB index initialization complete.");
    }

    private void ensureAccountIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps(Account.class);

        // Unique Account Number Index
        indexOps.ensureIndex(new Index().on("accountNumber", Sort.Direction.ASC).unique().named("acc_num_unique_idx"));

        // Compound Index: customerId + status
        Document compoundKeys = new Document("customerId", 1).append("status", 1);
        indexOps.ensureIndex(new CompoundIndexDefinition(compoundKeys).named("acc_cust_status_prog_idx"));
    }

    private void ensureLedgerIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps(LedgerEntry.class);

        // Compound Index: customerId + timestamp desc
        Document custTimeKeys = new Document("customerId", 1).append("timestamp", -1);
        indexOps.ensureIndex(new CompoundIndexDefinition(custTimeKeys).named("ledger_cust_time_prog_idx"));

        // Full-Text Search Index on description and notes with relative weighting
        TextIndexDefinition textIndex = new TextIndexDefinition.TextIndexDefinitionBuilder()
                .named("ledger_text_search_idx")
                .onField("description", 3.0f)
                .onField("notes", 1.0f)
                .build();
        indexOps.ensureIndex(textIndex);
    }

    private void ensureEventIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps(EventDocument.class);

        // Unique Event ID Index
        indexOps.ensureIndex(new Index().on("eventId", Sort.Direction.ASC).unique().named("event_id_unique_idx"));

        // Compound Index: aggregateId + eventTimestamp
        Document aggTimeKeys = new Document("aggregateId", 1).append("eventTimestamp", -1);
        indexOps.ensureIndex(new CompoundIndexDefinition(aggTimeKeys).named("event_agg_time_prog_idx"));

        // Time-To-Live (TTL) Index: evicts documents where now >= expireAt
        indexOps.ensureIndex(new Index()
                .on("expireAt", Sort.Direction.ASC)
                .expire(0, TimeUnit.SECONDS)
                .named("event_ttl_prog_idx"));
    }

    private void ensureCustomerIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps(Customer.class);
        indexOps.ensureIndex(new Index().on("customerNumber", Sort.Direction.ASC).unique().named("cust_num_unique_idx"));
        indexOps.ensureIndex(new Index().on("email", Sort.Direction.ASC).unique().named("cust_email_unique_idx"));
    }
}
