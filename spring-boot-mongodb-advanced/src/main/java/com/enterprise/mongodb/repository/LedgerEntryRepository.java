package com.enterprise.mongodb.repository;

import com.enterprise.mongodb.domain.model.LedgerEntry;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LedgerEntryRepository extends MongoRepository<LedgerEntry, String> {

    List<LedgerEntry> findByCustomerIdOrderByTimestampDesc(String customerId);

    List<LedgerEntry> findByAccountId(String accountId);

    /**
     * Executes full-text search using MongoDB Text Index.
     */
    List<LedgerEntry> findAllBy(TextCriteria criteria);
}
