package com.enterprise.mongodb.service.impl;

import com.enterprise.mongodb.domain.common.Money;
import com.enterprise.mongodb.domain.model.Account;
import com.enterprise.mongodb.domain.model.AccountStatus;
import com.enterprise.mongodb.domain.model.LedgerEntry;
import com.enterprise.mongodb.domain.model.LedgerEntryType;
import com.enterprise.mongodb.repository.AccountRepository;
import com.enterprise.mongodb.repository.LedgerEntryRepository;
import com.enterprise.mongodb.service.AccountTransferService;
import com.enterprise.mongodb.web.dto.AccountDto;
import com.enterprise.mongodb.web.dto.CreateAccountRequest;
import com.enterprise.mongodb.web.dto.TransferRequest;
import com.enterprise.mongodb.web.dto.TransferResponse;
import com.enterprise.mongodb.web.exception.InsufficientFundsException;
import com.enterprise.mongodb.web.exception.InvalidTransactionException;
import com.enterprise.mongodb.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountTransferServiceImpl implements AccountTransferService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 150, multiplier = 2.0)
    )
    public TransferResponse transferFunds(TransferRequest request) {
        log.info("Initiating atomic transfer from '{}' to '{}' of amount {} {}",
                request.getFromAccountNumber(), request.getToAccountNumber(), request.getAmount(), request.getCurrency());

        if (request.getFromAccountNumber().equalsIgnoreCase(request.getToAccountNumber())) {
            throw new InvalidTransactionException("Source and destination accounts cannot be identical");
        }

        Currency currency = Currency.getInstance(request.getCurrency());
        Money transferAmount = Money.of(request.getAmount(), currency);

        // Fetch source and destination accounts
        Account fromAccount = accountRepository.findByAccountNumber(request.getFromAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account", request.getFromAccountNumber()));

        Account toAccount = accountRepository.findByAccountNumber(request.getToAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account", request.getToAccountNumber()));

        // Validate account operational states
        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidTransactionException("Source account " + fromAccount.getAccountNumber() + " is not ACTIVE");
        }
        if (toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidTransactionException("Destination account " + toAccount.getAccountNumber() + " is not ACTIVE");
        }

        // Validate currency alignment
        if (!fromAccount.getBalance().getCurrency().equals(currency) ||
                !toAccount.getBalance().getCurrency().equals(currency)) {
            throw new InvalidTransactionException(String.format("Cross-currency transfer not supported. Expected: %s", currency));
        }

        // Enforce liquidity constraint
        if (!fromAccount.getBalance().isGreaterThanOrEqual(transferAmount)) {
            throw new InsufficientFundsException(
                    fromAccount.getAccountNumber(),
                    transferAmount.getAmount().toPlainString(),
                    fromAccount.getBalance().getAmount().toPlainString()
            );
        }

        // 1. Debit Source Account
        fromAccount.setBalance(fromAccount.getBalance().subtract(transferAmount));
        Account savedFromAccount = accountRepository.save(fromAccount); // @Version incremented & checked

        // 2. Credit Destination Account
        toAccount.setBalance(toAccount.getBalance().add(transferAmount));
        accountRepository.save(toAccount); // @Version incremented & checked

        // 3. Generate Immutable Double-Entry Ledger Audit Records
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Instant now = Instant.now();

        LedgerEntry debitEntry = LedgerEntry.builder()
                .transactionId(transactionId)
                .accountId(fromAccount.getId())
                .customerId(fromAccount.getCustomerId())
                .type(LedgerEntryType.TRANSFER_OUT)
                .amount(transferAmount)
                .balanceAfter(savedFromAccount.getBalance())
                .description("Transfer out to " + toAccount.getAccountNumber())
                .notes(request.getDescription())
                .timestamp(now)
                .build();

        LedgerEntry creditEntry = LedgerEntry.builder()
                .transactionId(transactionId)
                .accountId(toAccount.getId())
                .customerId(toAccount.getCustomerId())
                .type(LedgerEntryType.TRANSFER_IN)
                .amount(transferAmount)
                .balanceAfter(toAccount.getBalance())
                .description("Transfer in from " + fromAccount.getAccountNumber())
                .notes(request.getDescription())
                .timestamp(now)
                .build();

        ledgerEntryRepository.save(debitEntry);
        ledgerEntryRepository.save(creditEntry);

        log.info("Successfully executed atomic transaction '{}'. Source remaining balance: {}",
                transactionId, savedFromAccount.getBalance().getAmount());

        return TransferResponse.builder()
                .transactionId(transactionId)
                .fromAccountNumber(fromAccount.getAccountNumber())
                .toAccountNumber(toAccount.getAccountNumber())
                .transferredAmount(transferAmount.getAmount())
                .currency(currency.getCurrencyCode())
                .fromRemainingBalance(savedFromAccount.getBalance().getAmount())
                .timestamp(now)
                .status("COMPLETED")
                .build();
    }

    @Override
    public AccountDto createAccount(CreateAccountRequest request) {
        if (accountRepository.existsByAccountNumber(request.getAccountNumber())) {
            throw new InvalidTransactionException("Account number " + request.getAccountNumber() + " already exists");
        }

        Currency currency = Currency.getInstance(request.getCurrency());
        Account account = Account.builder()
                .accountNumber(request.getAccountNumber())
                .customerId(request.getCustomerId())
                .balance(Money.of(request.getInitialDeposit(), currency))
                .status(AccountStatus.ACTIVE)
                .build();

        Account saved = accountRepository.save(account);
        return toDto(saved);
    }

    @Override
    public AccountDto getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountNumber));
        return toDto(account);
    }

    @Override
    public AccountDto updateBalanceDirectly(String accountNumber, BigDecimal amountDelta, Long expectedVersion) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountNumber));

        if (expectedVersion != null) {
            account.setVersion(expectedVersion);
        }

        account.setBalance(account.getBalance().add(Money.of(amountDelta, account.getBalance().getCurrency())));
        Account saved = accountRepository.save(account);
        return toDto(saved);
    }

    private AccountDto toDto(Account account) {
        return AccountDto.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomerId())
                .balance(account.getBalance().getAmount())
                .currency(account.getBalance().getCurrency().getCurrencyCode())
                .status(account.getStatus())
                .version(account.getVersion())
                .createdAt(account.getCreatedAt())
                .lastModifiedAt(account.getLastModifiedAt())
                .build();
    }
}
