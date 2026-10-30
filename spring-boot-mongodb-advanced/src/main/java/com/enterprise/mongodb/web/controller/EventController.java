package com.enterprise.mongodb.web.controller;

import com.enterprise.mongodb.domain.model.event.EventDocument;
import com.enterprise.mongodb.domain.model.event.PaymentEvent;
import com.enterprise.mongodb.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<EventDocument> recordEvent(@Valid @RequestBody EventDocument event) {
        EventDocument saved = eventService.recordEvent(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventDocument> getEvent(@PathVariable String eventId) {
        return ResponseEntity.ok(eventService.getEventById(eventId));
    }

    @GetMapping("/aggregate/{aggregateId}")
    public ResponseEntity<List<EventDocument>> getEventsByAggregate(@PathVariable String aggregateId) {
        return ResponseEntity.ok(eventService.getEventsForAggregate(aggregateId));
    }

    @GetMapping("/payments/customer/{customerId}")
    public ResponseEntity<List<PaymentEvent>> getPaymentEventsByCustomer(@PathVariable String customerId) {
        return ResponseEntity.ok(eventService.getPaymentEventsForCustomer(customerId));
    }
}
