package com.enterprise.mongodb.domain.model;

import com.enterprise.mongodb.domain.common.BaseAuditableDocument;
import com.enterprise.mongodb.domain.common.Money;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Account document featuring Optimistic Locking via @Version
 * and compound indexing for customer portfolio queries.
 */
@Getter
@Setter
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "accounts")
@CompoundIndexes({
        @CompoundIndex(name = "acc_cust_status_idx", def = "{'customerId': 1, 'status': 1}")
})
public class Account extends BaseAuditableDocument {

    @Indexed(unique = true)
    private String accountNumber;

    @Indexed
    private String customerId;

    private Money balance;

    private AccountStatus status;

    /**
     * Optimistic locking version field.
     * Automatically incremented and verified by Spring Data MongoDB on every save/update.
     */
    @Version
    private Long version;
}
