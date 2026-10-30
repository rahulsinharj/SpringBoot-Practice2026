package com.enterprise.mongodb.unit;

import com.enterprise.mongodb.domain.common.Money;
import com.enterprise.mongodb.domain.model.Account;
import com.enterprise.mongodb.domain.model.AccountStatus;
import com.enterprise.mongodb.domain.model.LedgerEntry;
import com.enterprise.mongodb.repository.AccountRepository;
import com.enterprise.mongodb.repository.LedgerEntryRepository;
import com.enterprise.mongodb.service.impl.AccountTransferServiceImpl;
import com.enterprise.mongodb.web.dto.TransferRequest;
import com.enterprise.mongodb.web.dto.TransferResponse;
import com.enterprise.mongodb.web.exception.InsufficientFundsException;
import com.enterprise.mongodb.web.exception.InvalidTransactionException;
import com.enterprise.mongodb.web.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountTransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks
    private AccountTransferServiceImpl transferService;

    private final Currency usd = Currency.getInstance("USD");

    @Test
    @DisplayName("Should successfully transfer funds between valid accounts and write dual ledger entries")
    void shouldTransferFundsSuccessfully() {
        Account source = Account.builder()
                .accountNumber("ACC-SRC")
                .customerId("CUST-1")
                .balance(Money.of(new BigDecimal("500.00"), usd))
                .status(AccountStatus.ACTIVE)
                .build();
        source.setId("id-src");

        Account dest = Account.builder()
                .accountNumber("ACC-DST")
                .customerId("CUST-2")
                .balance(Money.of(new BigDecimal("100.00"), usd))
                .status(AccountStatus.ACTIVE)
                .build();
        dest.setId("id-dst");

        when(accountRepository.findByAccountNumber("ACC-SRC")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumber("ACC-DST")).thenReturn(Optional.of(dest));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("ACC-SRC")
                .toAccountNumber("ACC-DST")
                .amount(new BigDecimal("200.00"))
                .currency("USD")
                .description("Vendor settlement")
                .build();

        TransferResponse response = transferService.transferFunds(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getFromRemainingBalance()).isEqualByComparingTo("300.00");
        assertThat(dest.getBalance().getAmount()).isEqualByComparingTo("300.00");

        verify(accountRepository, times(2)).save(any(Account.class));
        verify(ledgerEntryRepository, times(2)).save(any(LedgerEntry.class));
    }

    @Test
    @DisplayName("Should reject transfer when source account has insufficient liquidity")
    void shouldThrowInsufficientFunds() {
        Account source = Account.builder()
                .accountNumber("ACC-POOR")
                .balance(Money.of(new BigDecimal("10.00"), usd))
                .status(AccountStatus.ACTIVE)
                .build();

        Account dest = Account.builder()
                .accountNumber("ACC-RICH")
                .balance(Money.of(new BigDecimal("1000.00"), usd))
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findByAccountNumber("ACC-POOR")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumber("ACC-RICH")).thenReturn(Optional.of(dest));

        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("ACC-POOR")
                .toAccountNumber("ACC-RICH")
                .amount(new BigDecimal("50.00"))
                .currency("USD")
                .build();

        assertThatThrownBy(() -> transferService.transferFunds(request))
                .isInstanceOf(InsufficientFundsException.class);

        verify(accountRepository, never()).save(any());
        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject transfer when source and target account numbers are identical")
    void shouldThrowWhenAccountsIdentical() {
        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("ACC-SAME")
                .toAccountNumber("ACC-SAME")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .build();

        assertThatThrownBy(() -> transferService.transferFunds(request))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("cannot be identical");
    }

    @Test
    @DisplayName("Should reject transfer when either account does not exist")
    void shouldThrowWhenAccountNotFound() {
        when(accountRepository.findByAccountNumber("ACC-GHOST")).thenReturn(Optional.empty());

        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("ACC-GHOST")
                .toAccountNumber("ACC-REAL")
                .amount(new BigDecimal("50.00"))
                .currency("USD")
                .build();

        assertThatThrownBy(() -> transferService.transferFunds(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
