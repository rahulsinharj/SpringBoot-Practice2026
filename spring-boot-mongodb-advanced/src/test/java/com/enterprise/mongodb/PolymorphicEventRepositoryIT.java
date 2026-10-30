package com.enterprise.mongodb;

import com.enterprise.mongodb.domain.common.Money;
import com.enterprise.mongodb.domain.model.event.EventDocument;
import com.enterprise.mongodb.domain.model.event.PaymentEvent;
import com.enterprise.mongodb.domain.model.event.UserActivityEvent;
import com.enterprise.mongodb.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PolymorphicEventRepositoryIT extends BaseMongoIntegrationTest {

    @Autowired
    private EventRepository eventRepository;

    @BeforeEach
    void cleanUp() {
        eventRepository.deleteAll();
    }

    @Test
    @DisplayName("Should persist and polymorphically deserialize PaymentEvent and UserActivityEvent with auditing")
    void shouldPersistAndDeserializePolymorphicEvents() {
        String aggregateId = "AGG-" + UUID.randomUUID();
        Instant now = Instant.now();
        Instant ttlExpiry = now.plus(30, ChronoUnit.DAYS);

        // 1. Create and persist concrete PaymentEvent
        PaymentEvent paymentEvent = PaymentEvent.builder()
                .eventId("EVT-PAY-1001")
                .aggregateId(aggregateId)
                .aggregateType("ACCOUNT")
                .eventTimestamp(now)
                .expireAt(ttlExpiry)
                .amount(Money.of(new BigDecimal("499.99"), Currency.getInstance("USD")))
                .paymentMethod("CREDIT_CARD")
                .status("CAPTURED")
                .transactionReference("TXN-REF-8899")
                .customerId("CUST-900")
                .build();

        // 2. Create and persist concrete UserActivityEvent
        UserActivityEvent userEvent = UserActivityEvent.builder()
                .eventId("EVT-ACT-2002")
                .aggregateId(aggregateId)
                .aggregateType("ACCOUNT")
                .eventTimestamp(now.plusSeconds(5))
                .expireAt(ttlExpiry)
                .userId("USR-5544")
                .actionType("MFA_AUTHENTICATED")
                .ipAddress("192.168.1.100")
                .userAgent("Mozilla/5.0")
                .metadata(Map.of("device", "macOS", "browser", "Chrome"))
                .build();

        eventRepository.save(paymentEvent);
        eventRepository.save(userEvent);

        // 3. Query base polymorphic collection
        List<EventDocument> events = eventRepository.findByAggregateIdOrderByEventTimestampDesc(aggregateId);

        assertThat(events).hasSize(2);

        // Verify TypeAlias mapping and subclass casting
        EventDocument first = events.get(0);
        EventDocument second = events.get(1);

        assertThat(first).isInstanceOf(UserActivityEvent.class);
        UserActivityEvent act = (UserActivityEvent) first;
        assertThat(act.getActionType()).isEqualTo("MFA_AUTHENTICATED");
        assertThat(act.getMetadata()).containsEntry("device", "macOS");

        assertThat(second).isInstanceOf(PaymentEvent.class);
        PaymentEvent pay = (PaymentEvent) second;
        assertThat(pay.getAmount().getAmount()).isEqualByComparingTo("499.99");
        assertThat(pay.getPaymentMethod()).isEqualTo("CREDIT_CARD");

        // Verify Auditing fields were automatically populated
        assertThat(pay.getCreatedAt()).isNotNull();
        assertThat(pay.getCreatedBy()).isEqualTo("SYSTEM_PROCESS");

        // 4. Query specifically for payments using TypeAlias filter
        List<PaymentEvent> customerPayments = eventRepository.findPaymentEventsByCustomerId("CUST-900");
        assertThat(customerPayments).hasSize(1);
        assertThat(customerPayments.get(0).getTransactionReference()).isEqualTo("TXN-REF-8899");
    }
}
