package com.enterprise.mongodb.repository;

import com.enterprise.mongodb.domain.model.Account;
import com.enterprise.mongodb.domain.model.AccountStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends MongoRepository<Account, String> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByCustomerIdAndStatus(String customerId, AccountStatus status);

    boolean existsByAccountNumber(String accountNumber);
}
