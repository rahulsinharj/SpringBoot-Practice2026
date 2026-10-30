package com.enterprise.mongodb.repository.aggregation;

import com.enterprise.mongodb.web.dto.CustomerSpendingSummary;

import java.time.Instant;
import java.util.List;

public interface AnalyticsRepository {

    /**
     * Executes a complex aggregation pipeline ($match, $lookup, $unwind, $group, $project, $sort, $limit)
     * consolidating transaction metrics joined with customer domain records.
     *
     * @param fromDate lower boundary timestamp
     * @param toDate upper boundary timestamp
     * @param limit maximum top spenders to return
     * @return List of aggregated customer spending summaries
     */
    List<CustomerSpendingSummary> aggregateTopSpenders(Instant fromDate, Instant toDate, int limit);
}
