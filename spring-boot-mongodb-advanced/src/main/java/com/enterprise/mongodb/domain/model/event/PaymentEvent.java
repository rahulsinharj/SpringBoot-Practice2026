package com.enterprise.mongodb.domain.model.event;

import com.enterprise.mongodb.domain.common.Money;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.TypeAlias;

import java.time.Instant;

/**
 * Concrete Payment Event representation in polymorphic collection.
 */
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
@TypeAlias("PAYMENT")
public class PaymentEvent extends EventDocument {

    private Money amount;
    private String paymentMethod;
    private String status;
    private String transactionReference;
    private String customerId;

    @Builder
    public PaymentEvent(String eventId, String aggregateId, String aggregateType,
                        Instant eventTimestamp, Instant expireAt, Money amount,
                        String paymentMethod, String status, String transactionReference,
                        String customerId) {
        super(eventId, aggregateId, aggregateType, eventTimestamp, expireAt);
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.transactionReference = transactionReference;
        this.customerId = customerId;
    }
}
