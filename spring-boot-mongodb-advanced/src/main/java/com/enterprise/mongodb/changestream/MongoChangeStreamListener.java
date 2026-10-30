package com.enterprise.mongodb.changestream;

import com.enterprise.mongodb.domain.model.Account;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.client.model.changestream.OperationType;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonTimestamp;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.messaging.ChangeStreamRequest;
import org.springframework.data.mongodb.core.messaging.DefaultMessageListenerContainer;
import org.springframework.data.mongodb.core.messaging.Message;
import org.springframework.data.mongodb.core.messaging.MessageListener;
import org.springframework.data.mongodb.core.messaging.MessageListenerContainer;
import org.springframework.data.mongodb.core.messaging.Subscription;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Enterprise Production Change Stream Listener.
 * Connects to MongoDB oplog via Change Streams and dispatches real-time events asynchronously.
 * Operates across application lifecycle with automatic thread management and resume token capture.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.mongodb.change-stream", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MongoChangeStreamListener implements MessageListener<ChangeStreamDocument<Document>, Account> {

    private final MongoTemplate mongoTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.mongodb.change-stream.collection-name:accounts}")
    private String collectionName;

    private MessageListenerContainer container;
    private Subscription subscription;

    @PostConstruct
    public void startListening() {
        try {
            log.info("Initializing MongoDB Change Stream Listener on collection '{}'...", collectionName);

            container = new DefaultMessageListenerContainer(mongoTemplate);
            container.start();

            ChangeStreamRequest<Account> request = ChangeStreamRequest.builder(this)
                    .collection(collectionName)
                    .fullDocumentLookup(FullDocument.UPDATE_LOOKUP)
                    .build();

            subscription = container.register(request, Account.class);
            log.info("MongoDB Change Stream Listener successfully subscribed to '{}'", collectionName);
        } catch (Exception ex) {
            log.warn("Could not start MongoDB Change Stream (requires MongoDB Replica Set): {}. Continuing startup.",
                    ex.getMessage());
        }
    }

    @Override
    public void onMessage(Message<ChangeStreamDocument<Document>, Account> message) {
        ChangeStreamDocument<Document> rawChange = message.getRaw();
        Account fullDocument = message.getBody();

        if (rawChange == null) {
            log.warn("Received empty change stream message");
            return;
        }

        OperationType operationType = rawChange.getOperationType();
        String documentKey = rawChange.getDocumentKey() != null ? rawChange.getDocumentKey().toJson() : "UNKNOWN";
        BsonTimestamp clusterTime = rawChange.getClusterTime();
        Instant eventTime = clusterTime != null ? Instant.ofEpochSecond(clusterTime.getTime()) : Instant.now();

        log.info("⚡ [Change Stream] Captured Mutation on '{}' | Operation: {} | Key: {}",
                collectionName, operationType, documentKey);

        if (fullDocument != null) {
            log.debug("⚡ [Change Stream] Hydrated Document State: Account={}, Balance={}",
                    fullDocument.getAccountNumber(), fullDocument.getBalance());
        }

        // Publish to internal Spring ApplicationEvent bus for downstream asynchronous consumption
        AccountChangeEvent changeEvent = AccountChangeEvent.builder()
                .accountId(documentKey)
                .operationType(operationType)
                .timestamp(eventTime)
                .rawResumeToken(rawChange.getResumeToken() != null ? rawChange.getResumeToken().toJson() : null)
                .fullDocument(fullDocument)
                .build();

        eventPublisher.publishEvent(changeEvent);
    }

    @PreDestroy
    public void stopListening() {
        log.info("Shutting down MongoDB Change Stream listener...");
        if (container != null && subscription != null) {
            container.remove(subscription);
            container.stop();
        }
    }
}
