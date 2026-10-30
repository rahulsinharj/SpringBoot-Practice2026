package com.enterprise.mongodb.service;

import com.enterprise.mongodb.domain.model.event.EventDocument;
import com.enterprise.mongodb.domain.model.event.PaymentEvent;

import java.util.List;

public interface EventService {

    EventDocument recordEvent(EventDocument event);

    EventDocument getEventById(String eventId);

    List<EventDocument> getEventsForAggregate(String aggregateId);

    List<PaymentEvent> getPaymentEventsForCustomer(String customerId);
}
