package com.enterprise.mongodb.domain.model;

import com.enterprise.mongodb.domain.common.BaseAuditableDocument;
import com.enterprise.mongodb.domain.common.Money;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Immutable financial ledger entry representing an atomic credit or debit.
 * Equipped with compound indexing and full-text search indexing on description and notes.
 */
@Getter
@Setter
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ledger_entries")
@CompoundIndexes({
        @CompoundIndex(name = "ledger_cust_time_idx", def = "{'customerId': 1, 'timestamp': -1}"),
        @CompoundIndex(name = "ledger_acc_type_idx", def = "{'accountId': 1, 'type': 1}")
})
public class LedgerEntry extends BaseAuditableDocument {

    @Indexed
    private String transactionId;

    @Indexed
    private String accountId;

    @Indexed
    private String customerId;

    private LedgerEntryType type;

    private Money amount;

    private Money balanceAfter;

    @TextIndexed(weight = 3)
    private String description;

    @TextIndexed(weight = 1)
    private String notes;

    @Indexed
    private Instant timestamp;
}
