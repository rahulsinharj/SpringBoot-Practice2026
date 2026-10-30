package com.enterprise.mongodb.web.dto;

import com.enterprise.mongodb.domain.model.AccountStatus;
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
public class AccountDto {

    private String id;
    private String accountNumber;
    private String customerId;
    private BigDecimal balance;
    private String currency;
    private AccountStatus status;
    private Long version;
    private Instant createdAt;
    private Instant lastModifiedAt;
}
