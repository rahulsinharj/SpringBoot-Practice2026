package com.enterprise.mongodb.changestream;

import com.mongodb.client.model.changestream.OperationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event published internally when a change stream captures an account mutation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountChangeEvent {

    private String accountId;
    private OperationType operationType;
    private Instant timestamp;
    private String rawResumeToken;
    private Object fullDocument;
}
