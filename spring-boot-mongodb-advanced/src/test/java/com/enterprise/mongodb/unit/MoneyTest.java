package com.enterprise.mongodb.unit;

import com.enterprise.mongodb.domain.common.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    private final Currency usd = Currency.getInstance("USD");
    private final Currency eur = Currency.getInstance("EUR");

    @Test
    @DisplayName("Should correctly perform addition and subtraction with high precision")
    void shouldPerformArithmetic() {
        Money m1 = Money.of(new BigDecimal("100.50"), usd);
        Money m2 = Money.of(new BigDecimal("49.50"), usd);

        Money sum = m1.add(m2);
        assertThat(sum.getAmount()).isEqualByComparingTo("150.00");
        assertThat(sum.getCurrency()).isEqualTo(usd);

        Money diff = m1.subtract(m2);
        assertThat(diff.getAmount()).isEqualByComparingTo("51.00");
    }

    @Test
    @DisplayName("Should prevent arithmetic operations across disparate currencies")
    void shouldThrowOnCurrencyMismatch() {
        Money dollar = Money.of(new BigDecimal("100.00"), usd);
        Money euro = Money.of(new BigDecimal("100.00"), eur);

        assertThatThrownBy(() -> dollar.add(euro))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency mismatch");
    }

    @Test
    @DisplayName("Should accurately compare monetary values")
    void shouldCompareMonetaryValues() {
        Money smaller = Money.of(new BigDecimal("25.00"), usd);
        Money larger = Money.of(new BigDecimal("50.00"), usd);

        assertThat(larger.isGreaterThanOrEqual(smaller)).isTrue();
        assertThat(smaller.isGreaterThanOrEqual(larger)).isFalse();
        assertThat(larger.compareTo(smaller)).isGreaterThan(0);
    }
}
