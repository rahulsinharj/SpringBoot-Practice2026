package com.enterprise.mongodb.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Aggregation Projection DTO representing consolidated customer financial metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSpendingSummary {

    private String customerId;
    private String customerName;
    private String customerEmail;
    private String customerTier;
    private BigDecimal totalDebitAmount;
    private BigDecimal averageDebitAmount;
    private Long transactionCount;
    private BigDecimal maxDebitAmount;
    private String currency;
}
