package com.enterprise.mongodb.domain.model.event;

import com.enterprise.mongodb.domain.common.BaseAuditableDocument;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Polymorphic Event Document Base Class stored in the 'domain_events' collection.
 * Utilizes Spring Data TypeAlias and Jackson Polymorphic type deserialization.
 */
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
@Document(collection = "domain_events")
@CompoundIndexes({
        @CompoundIndex(name = "event_agg_time_idx", def = "{'aggregateId': 1, 'eventTimestamp': -1}"),
        @CompoundIndex(name = "event_type_agg_idx", def = "{'_class': 1, 'aggregateType': 1}")
})
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PaymentEvent.class, name = "PAYMENT"),
        @JsonSubTypes.Type(value = UserActivityEvent.class, name = "USER_ACTIVITY")
})
public abstract class EventDocument extends BaseAuditableDocument {

    @Indexed(unique = true)
    private String eventId;

    private String aggregateId;

    private String aggregateType;

    private Instant eventTimestamp;

    /**
     * TTL index: documents will automatically be evicted by MongoDB background thread
     * when current time exceeds expireAt.
     */
    @Indexed(name = "event_ttl_idx", expireAfter = "0s")
    private Instant expireAt;

    protected EventDocument(String eventId, String aggregateId, String aggregateType, Instant eventTimestamp, Instant expireAt) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventTimestamp = eventTimestamp != null ? eventTimestamp : Instant.now();
        this.expireAt = expireAt;
    }
}
