package com.enterprise.mongodb.domain.model.event;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.TypeAlias;

import java.time.Instant;
import java.util.Map;

/**
 * Concrete User Activity Event representation in polymorphic collection.
 */
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
@TypeAlias("USER_ACTIVITY")
public class UserActivityEvent extends EventDocument {

    private String userId;
    private String actionType;
    private String ipAddress;
    private String userAgent;
    private Map<String, Object> metadata;

    @Builder
    public UserActivityEvent(String eventId, String aggregateId, String aggregateType,
                             Instant eventTimestamp, Instant expireAt, String userId,
                             String actionType, String ipAddress, String userAgent,
                             Map<String, Object> metadata) {
        super(eventId, aggregateId, aggregateType, eventTimestamp, expireAt);
        this.userId = userId;
        this.actionType = actionType;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.metadata = metadata;
    }
}
