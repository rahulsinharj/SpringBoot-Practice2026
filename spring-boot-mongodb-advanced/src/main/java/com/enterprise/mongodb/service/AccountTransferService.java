package com.enterprise.mongodb.service;

import com.enterprise.mongodb.web.dto.AccountDto;
import com.enterprise.mongodb.web.dto.CreateAccountRequest;
import com.enterprise.mongodb.web.dto.TransferRequest;
import com.enterprise.mongodb.web.dto.TransferResponse;

public interface AccountTransferService {

    /**
     * Executes atomic, multi-document ACID financial transfer across accounts and ledger collections.
     * Protected by optimistic locking (@Version) and automated retry semantics.
     */
    TransferResponse transferFunds(TransferRequest request);

    AccountDto createAccount(CreateAccountRequest request);

    AccountDto getAccountByNumber(String accountNumber);

    /**
     * Updates an account balance directly; used to test and demonstrate concurrent optimistic locking collisions.
     */
    AccountDto updateBalanceDirectly(String accountNumber, java.math.BigDecimal amountDelta, Long expectedVersion);
}
