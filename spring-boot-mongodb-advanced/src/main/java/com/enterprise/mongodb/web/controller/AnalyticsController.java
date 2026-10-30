package com.enterprise.mongodb.web.controller;

import com.enterprise.mongodb.service.AnalyticsService;
import com.enterprise.mongodb.web.dto.CustomerSpendingSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/top-spenders")
    public ResponseEntity<List<CustomerSpendingSummary>> getTopSpenders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @RequestParam(defaultValue = "10") int limit) {
        List<CustomerSpendingSummary> summaries = analyticsService.getTopSpenders(fromDate, toDate, limit);
        return ResponseEntity.ok(summaries);
    }
}
