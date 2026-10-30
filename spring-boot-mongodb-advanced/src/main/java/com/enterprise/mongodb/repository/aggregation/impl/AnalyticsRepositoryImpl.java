package com.enterprise.mongodb.repository.aggregation.impl;

import com.enterprise.mongodb.domain.model.LedgerEntryType;
import com.enterprise.mongodb.repository.aggregation.AnalyticsRepository;
import com.enterprise.mongodb.web.dto.CustomerSpendingSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * High-performance enterprise aggregation pipeline implementation.
 * Chains $match, $lookup, $unwind, $group, $project, $sort, and $limit.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AnalyticsRepositoryImpl implements AnalyticsRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<CustomerSpendingSummary> aggregateTopSpenders(Instant fromDate, Instant toDate, int limit) {
        log.debug("Building aggregation pipeline for top spenders between {} and {}", fromDate, toDate);

        // 1. $match stage: Filter debit & transfer out transactions within target time window
        Criteria matchCriteria = Criteria.where("type").in(
                LedgerEntryType.DEBIT.name(),
                LedgerEntryType.TRANSFER_OUT.name()
        );

        if (fromDate != null && toDate != null) {
            matchCriteria.and("timestamp").gte(fromDate).lte(toDate);
        } else if (fromDate != null) {
            matchCriteria.and("timestamp").gte(fromDate);
        } else if (toDate != null) {
            matchCriteria.and("timestamp").lte(toDate);
        }

        MatchOperation matchStage = Aggregation.match(matchCriteria);

        // 2. $lookup stage: Cross-collection join with customers collection
        LookupOperation lookupStage = LookupOperation.newLookup()
                .from("customers")
                .localField("customerId")
                .foreignField("customerNumber")
                .as("customerDetails");

        // 3. $unwind stage: Flatten joined customer details
        UnwindOperation unwindStage = Aggregation.unwind("customerDetails", false);

        // 4. $group stage: Aggregate metrics per customer
        GroupOperation groupStage = Aggregation.group("customerId")
                .first("customerDetails.fullName").as("customerName")
                .first("customerDetails.email").as("customerEmail")
                .first("customerDetails.tier").as("customerTier")
                .first("amount.currency").as("currency")
                .sum("amount.amount").as("totalDebitAmount")
                .avg("amount.amount").as("averageDebitAmount")
                .count().as("transactionCount")
                .max("amount.amount").as("maxDebitAmount");

        // 5. $project stage: Format reporting schema
        ProjectionOperation projectStage = Aggregation.project()
                .and("_id").as("customerId")
                .and("customerName").as("customerName")
                .and("customerEmail").as("customerEmail")
                .and("customerTier").as("customerTier")
                .and("currency").as("currency")
                .and("totalDebitAmount").as("totalDebitAmount")
                .and("averageDebitAmount").as("averageDebitAmount")
                .and("transactionCount").as("transactionCount")
                .and("maxDebitAmount").as("maxDebitAmount");

        // 6. $sort stage: Order descending by total spend volume
        SortOperation sortStage = Aggregation.sort(Sort.Direction.DESC, "totalDebitAmount");

        // 7. Assemble full pipeline
        Aggregation aggregation = Aggregation.newAggregation(
                matchStage,
                lookupStage,
                unwindStage,
                groupStage,
                projectStage,
                sortStage,
                Aggregation.limit(limit > 0 ? limit : 10)
        );

        AggregationResults<CustomerSpendingSummary> results = mongoTemplate.aggregate(
                aggregation,
                "ledger_entries",
                CustomerSpendingSummary.class
        );

        return results.getMappedResults();
    }
}
