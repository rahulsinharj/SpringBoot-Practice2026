package com.enterprise.mongodb.service.impl;

import com.enterprise.mongodb.repository.aggregation.AnalyticsRepository;
import com.enterprise.mongodb.service.AnalyticsService;
import com.enterprise.mongodb.web.dto.CustomerSpendingSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final AnalyticsRepository analyticsRepository;

    @Override
    public List<CustomerSpendingSummary> getTopSpenders(Instant fromDate, Instant toDate, int limit) {
        return analyticsRepository.aggregateTopSpenders(fromDate, toDate, limit);
    }
}
