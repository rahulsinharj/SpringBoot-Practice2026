package com.enterprise.mongodb;

import com.enterprise.mongodb.domain.common.Money;
import com.enterprise.mongodb.domain.model.Customer;
import com.enterprise.mongodb.domain.model.LedgerEntry;
import com.enterprise.mongodb.domain.model.LedgerEntryType;
import com.enterprise.mongodb.repository.CustomerRepository;
import com.enterprise.mongodb.repository.LedgerEntryRepository;
import com.enterprise.mongodb.service.AnalyticsService;
import com.enterprise.mongodb.web.dto.CustomerSpendingSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ComplexAggregationIT extends BaseMongoIntegrationTest {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @BeforeEach
    void cleanUp() {
        ledgerEntryRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Test
    @DisplayName("Complex Aggregation: Pipeline with $match, $lookup, $unwind, $group, $project, $sort")
    void shouldExecuteComplexAggregationPipeline() {
        Currency usd = Currency.getInstance("USD");
        Instant now = Instant.now();

        // 1. Seed Customers in 'customers' collection
        Customer alice = customerRepository.save(Customer.builder()
                .customerNumber("CUST-ALICE")
                .fullName("Alice Wonderland")
                .email("alice@enterprise.com")
                .tier("PLATINUM")
                .build());

        Customer bob = customerRepository.save(Customer.builder()
                .customerNumber("CUST-BOB")
                .fullName("Bob Builder")
                .email("bob@enterprise.com")
                .tier("SILVER")
                .build());

        // 2. Seed Ledger Entries in 'ledger_entries' collection
        // Alice: 2 debits (150.00, 350.00) => Total = 500.00, Avg = 250.00, Count = 2, Max = 350.00
        ledgerEntryRepository.save(LedgerEntry.builder()
                .transactionId("TX-101")
                .accountId("ACC-ALICE-1")
                .customerId(alice.getCustomerNumber())
                .type(LedgerEntryType.DEBIT)
                .amount(Money.of(new BigDecimal("150.00"), usd))
                .timestamp(now.minus(2, ChronoUnit.DAYS))
                .build());

        ledgerEntryRepository.save(LedgerEntry.builder()
                .transactionId("TX-102")
                .accountId("ACC-ALICE-1")
                .customerId(alice.getCustomerNumber())
                .type(LedgerEntryType.TRANSFER_OUT)
                .amount(Money.of(new BigDecimal("350.00"), usd))
                .timestamp(now.minus(1, ChronoUnit.DAYS))
                .build());

        // Bob: 1 debit (200.00) => Total = 200.00, Avg = 200.00, Count = 1, Max = 200.00
        ledgerEntryRepository.save(LedgerEntry.builder()
                .transactionId("TX-201")
                .accountId("ACC-BOB-1")
                .customerId(bob.getCustomerNumber())
                .type(LedgerEntryType.DEBIT)
                .amount(Money.of(new BigDecimal("200.00"), usd))
                .timestamp(now.minus(1, ChronoUnit.DAYS))
                .build());

        // Also add a CREDIT transaction for Alice (should be excluded by $match stage)
        ledgerEntryRepository.save(LedgerEntry.builder()
                .transactionId("TX-103")
                .accountId("ACC-ALICE-1")
                .customerId(alice.getCustomerNumber())
                .type(LedgerEntryType.CREDIT)
                .amount(Money.of(new BigDecimal("1000.00"), usd))
                .timestamp(now.minus(1, ChronoUnit.DAYS))
                .build());

        // 3. Execute Aggregation Pipeline
        List<CustomerSpendingSummary> summaries = analyticsService.getTopSpenders(
                now.minus(7, ChronoUnit.DAYS),
                now.plus(1, ChronoUnit.DAYS),
                10
        );

        // 4. Assert Pipeline Results
        assertThat(summaries).hasSize(2);

        // Sorted by totalDebitAmount DESC: Alice first, Bob second
        CustomerSpendingSummary topSpender = summaries.get(0);
        assertThat(topSpender.getCustomerId()).isEqualTo("CUST-ALICE");
        assertThat(topSpender.getCustomerName()).isEqualTo("Alice Wonderland");
        assertThat(topSpender.getCustomerEmail()).isEqualTo("alice@enterprise.com");
        assertThat(topSpender.getCustomerTier()).isEqualTo("PLATINUM");
        assertThat(topSpender.getTotalDebitAmount()).isEqualByComparingTo("500.00");
        assertThat(topSpender.getAverageDebitAmount()).isEqualByComparingTo("250.00");
        assertThat(topSpender.getTransactionCount()).isEqualTo(2L);
        assertThat(topSpender.getMaxDebitAmount()).isEqualByComparingTo("350.00");

        CustomerSpendingSummary secondSpender = summaries.get(1);
        assertThat(secondSpender.getCustomerId()).isEqualTo("CUST-BOB");
        assertThat(secondSpender.getCustomerName()).isEqualTo("Bob Builder");
        assertThat(secondSpender.getTotalDebitAmount()).isEqualByComparingTo("200.00");
        assertThat(secondSpender.getTransactionCount()).isEqualTo(1L);
    }
}
