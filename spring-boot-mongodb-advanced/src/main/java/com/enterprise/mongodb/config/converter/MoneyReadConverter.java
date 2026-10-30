package com.enterprise.mongodb.config.converter;

import com.enterprise.mongodb.domain.common.Money;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * Custom reading converter hydrating BSON Document into Money Value Object.
 */
@ReadingConverter
public class MoneyReadConverter implements Converter<Document, Money> {

    @Override
    public Money convert(@NonNull Document source) {
        Object rawAmount = source.get("amount");
        BigDecimal amount;

        if (rawAmount instanceof Decimal128 decimal128) {
            amount = decimal128.bigDecimalValue();
        } else if (rawAmount instanceof Number number) {
            amount = BigDecimal.valueOf(number.doubleValue());
        } else if (rawAmount != null) {
            amount = new BigDecimal(rawAmount.toString());
        } else {
            amount = BigDecimal.ZERO;
        }

        String currencyCode = source.getString("currency");
        Currency currency = (currencyCode != null && !currencyCode.isBlank())
                ? Currency.getInstance(currencyCode)
                : Money.DEFAULT_CURRENCY;

        return new Money(amount, currency);
    }
}
