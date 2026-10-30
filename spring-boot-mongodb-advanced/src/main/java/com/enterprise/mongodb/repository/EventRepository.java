package com.enterprise.mongodb.repository;

import com.enterprise.mongodb.domain.model.event.EventDocument;
import com.enterprise.mongodb.domain.model.event.PaymentEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data Repository for Polymorphic Event Documents.
 */
@Repository
public interface EventRepository extends MongoRepository<EventDocument, String> {

    Optional<EventDocument> findByEventId(String eventId);

    List<EventDocument> findByAggregateIdOrderByEventTimestampDesc(String aggregateId);

    /**
     * Query specifically filtering polymorphic documents with TypeAlias 'PAYMENT'.
     */
    @Query("{ '_class': 'PAYMENT', 'customerId': ?0 }")
    List<PaymentEvent> findPaymentEventsByCustomerId(String customerId);

    /**
     * Query events by aggregate type with automatic concrete type resolution.
     */
    <T extends EventDocument> List<T> findByAggregateType(String aggregateType, Class<T> clazz);
}
