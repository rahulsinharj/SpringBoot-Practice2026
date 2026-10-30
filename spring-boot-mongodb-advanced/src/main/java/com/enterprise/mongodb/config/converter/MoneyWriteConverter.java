package com.enterprise.mongodb.config.converter;

import com.enterprise.mongodb.domain.common.Money;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.lang.NonNull;

/**
 * Custom writing converter serializing Money Value Object into BSON Document
 * using BSON Decimal128 for high-precision financial arithmetic in MongoDB.
 */
@WritingConverter
public class MoneyWriteConverter implements Converter<Money, Document> {

    @Override
    public Document convert(@NonNull Money source) {
        Document document = new Document();
        document.put("amount", new Decimal128(source.getAmount()));
        document.put("currency", source.getCurrency().getCurrencyCode());
        return document;
    }
}
