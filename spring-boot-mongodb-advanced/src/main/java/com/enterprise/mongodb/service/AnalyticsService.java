package com.enterprise.mongodb.service;

import com.enterprise.mongodb.web.dto.CustomerSpendingSummary;

import java.time.Instant;
import java.util.List;

public interface AnalyticsService {

    List<CustomerSpendingSummary> getTopSpenders(Instant fromDate, Instant toDate, int limit);
}
