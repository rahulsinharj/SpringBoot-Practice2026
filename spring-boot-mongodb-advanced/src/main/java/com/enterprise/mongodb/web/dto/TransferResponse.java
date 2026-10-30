package com.enterprise.mongodb.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {

    private String transactionId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal transferredAmount;
    private String currency;
    private BigDecimal fromRemainingBalance;
    private Instant timestamp;
    private String status;
}
