package com.enterprise.mongodb.service.impl;

import com.enterprise.mongodb.domain.model.event.EventDocument;
import com.enterprise.mongodb.domain.model.event.PaymentEvent;
import com.enterprise.mongodb.repository.EventRepository;
import com.enterprise.mongodb.service.EventService;
import com.enterprise.mongodb.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;

    @Override
    public EventDocument recordEvent(EventDocument event) {
        log.info("Persisting polymorphic event: id={}, type={}", event.getEventId(), event.getClass().getSimpleName());
        return eventRepository.save(event);
    }

    @Override
    public EventDocument getEventById(String eventId) {
        return eventRepository.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));
    }

    @Override
    public List<EventDocument> getEventsForAggregate(String aggregateId) {
        return eventRepository.findByAggregateIdOrderByEventTimestampDesc(aggregateId);
    }

    @Override
    public List<PaymentEvent> getPaymentEventsForCustomer(String customerId) {
        return eventRepository.findPaymentEventsByCustomerId(customerId);
    }
}
