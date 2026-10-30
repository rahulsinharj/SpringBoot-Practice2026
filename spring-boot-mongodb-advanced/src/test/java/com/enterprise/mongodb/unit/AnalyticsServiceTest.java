package com.enterprise.mongodb.unit;

import com.enterprise.mongodb.repository.aggregation.AnalyticsRepository;
import com.enterprise.mongodb.service.impl.AnalyticsServiceImpl;
import com.enterprise.mongodb.web.dto.CustomerSpendingSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private AnalyticsRepository analyticsRepository;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    @Test
    @DisplayName("Should delegate top spender aggregation to repository")
    void shouldDelegateTopSpenderAggregation() {
        Instant from = Instant.now().minusSeconds(86400);
        Instant to = Instant.now();
        int limit = 5;

        CustomerSpendingSummary summary = CustomerSpendingSummary.builder()
                .customerId("CUST-10")
                .customerName("Corporate Client")
                .totalDebitAmount(new BigDecimal("12500.00"))
                .transactionCount(14L)
                .build();

        when(analyticsRepository.aggregateTopSpenders(from, to, limit)).thenReturn(List.of(summary));

        List<CustomerSpendingSummary> results = analyticsService.getTopSpenders(from, to, limit);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCustomerId()).isEqualTo("CUST-10");
        verify(analyticsRepository).aggregateTopSpenders(from, to, limit);
    }
}
