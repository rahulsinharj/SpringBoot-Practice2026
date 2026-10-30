package com.enterprise.mongodb.domain.model;

import com.enterprise.mongodb.domain.common.BaseAuditableDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Customer entity collection.
 */
@Getter
@Setter
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "customers")
public class Customer extends BaseAuditableDocument {

    @Indexed(unique = true)
    private String customerNumber;

    private String fullName;

    @Indexed(unique = true)
    private String email;

    private String tier; // e.g. BRONZE, SILVER, GOLD, PLATINUM
}
