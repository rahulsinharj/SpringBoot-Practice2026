package com.enterprise.mongodb;

import com.enterprise.mongodb.domain.common.Money;
import com.enterprise.mongodb.domain.model.Account;
import com.enterprise.mongodb.domain.model.AccountStatus;
import com.enterprise.mongodb.repository.AccountRepository;
import com.enterprise.mongodb.repository.LedgerEntryRepository;
import com.enterprise.mongodb.service.AccountTransferService;
import com.enterprise.mongodb.web.dto.TransferRequest;
import com.enterprise.mongodb.web.dto.TransferResponse;
import com.enterprise.mongodb.web.exception.InsufficientFundsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionAndLockingIT extends BaseMongoIntegrationTest {

    @Autowired
    private AccountTransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @BeforeEach
    void cleanUp() {
        ledgerEntryRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    @DisplayName("ACID Multi-Document Transaction: Atomically transfer balance and record dual ledger entries")
    void shouldExecuteAtomicTransferAcrossCollections() {
        Currency usd = Currency.getInstance("USD");

        Account source = accountRepository.save(Account.builder()
                .accountNumber("ACC-100")
                .customerId("CUST-1")
                .balance(Money.of(new BigDecimal("1000.00"), usd))
                .status(AccountStatus.ACTIVE)
                .build());

        Account destination = accountRepository.save(Account.builder()
                .accountNumber("ACC-200")
                .customerId("CUST-2")
                .balance(Money.of(new BigDecimal("250.00"), usd))
                .status(AccountStatus.ACTIVE)
                .build());

        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("ACC-100")
                .toAccountNumber("ACC-200")
                .amount(new BigDecimal("300.00"))
                .currency("USD")
                .description("Payment for consulting services")
                .build();

        TransferResponse response = transferService.transferFunds(request);

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getFromRemainingBalance()).isEqualByComparingTo("700.00");

        // Verify Account states in database
        Account updatedSource = accountRepository.findByAccountNumber("ACC-100").orElseThrow();
        Account updatedDest = accountRepository.findByAccountNumber("ACC-200").orElseThrow();

        assertThat(updatedSource.getBalance().getAmount()).isEqualByComparingTo("700.00");
        assertThat(updatedDest.getBalance().getAmount()).isEqualByComparingTo("550.00");

        // Verify Version increments
        assertThat(updatedSource.getVersion()).isGreaterThan(source.getVersion());
        assertThat(updatedDest.getVersion()).isGreaterThan(destination.getVersion());

        // Verify 2 atomic ledger entries created
        assertThat(ledgerEntryRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Transaction Rollback: Ensure atomicity on failure (no partial writes)")
    void shouldRollbackEntireTransactionOnFailure() {
        Currency usd = Currency.getInstance("USD");

        accountRepository.save(Account.builder()
                .accountNumber("ACC-300")
                .customerId("CUST-3")
                .balance(Money.of(new BigDecimal("50.00"), usd))
                .status(AccountStatus.ACTIVE)
                .build());

        accountRepository.save(Account.builder()
                .accountNumber("ACC-400")
                .customerId("CUST-4")
                .balance(Money.of(new BigDecimal("100.00"), usd))
                .status(AccountStatus.ACTIVE)
                .build());

        // Attempt transfer exceeding available funds (500 > 50)
        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("ACC-300")
                .toAccountNumber("ACC-400")
                .amount(new BigDecimal("500.00"))
                .currency("USD")
                .description("Should fail due to insufficient funds")
                .build();

        assertThatThrownBy(() -> transferService.transferFunds(request))
                .isInstanceOf(InsufficientFundsException.class);

        // Verify complete rollback: neither balance changed and no ledger entries persisted
        Account untouchedSource = accountRepository.findByAccountNumber("ACC-300").orElseThrow();
        Account untouchedDest = accountRepository.findByAccountNumber("ACC-400").orElseThrow();

        assertThat(untouchedSource.getBalance().getAmount()).isEqualByComparingTo("50.00");
        assertThat(untouchedDest.getBalance().getAmount()).isEqualByComparingTo("100.00");
        assertThat(ledgerEntryRepository.count()).isZero();
    }

    @Test
    @DisplayName("Optimistic Locking: Prevent concurrent updates using @Version collision detection")
    void shouldThrowOptimisticLockingFailureOnConcurrentModification() {
        Currency usd = Currency.getInstance("USD");

        Account account = accountRepository.save(Account.builder()
                .accountNumber("ACC-500")
                .customerId("CUST-5")
                .balance(Money.of(new BigDecimal("100.00"), usd))
                .status(AccountStatus.ACTIVE)
                .build());

        Long initialVersion = account.getVersion();

        // 1. Thread A updates the account successfully, incrementing the version
        transferService.updateBalanceDirectly("ACC-500", new BigDecimal("50.00"), initialVersion);

        // 2. Thread B attempts to update the account holding the old stale version
        assertThatThrownBy(() ->
                transferService.updateBalanceDirectly("ACC-500", new BigDecimal("25.00"), initialVersion)
        ).isInstanceOf(OptimisticLockingFailureException.class);

        // Verify balance reflects only Thread A's committed update
        Account finalAccount = accountRepository.findByAccountNumber("ACC-500").orElseThrow();
        assertThat(finalAccount.getBalance().getAmount()).isEqualByComparingTo("150.00");
    }
}
