package com.enterprise.mongodb.web.controller;

import com.enterprise.mongodb.service.AccountTransferService;
import com.enterprise.mongodb.web.dto.AccountDto;
import com.enterprise.mongodb.web.dto.CreateAccountRequest;
import com.enterprise.mongodb.web.dto.TransferRequest;
import com.enterprise.mongodb.web.dto.TransferResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountTransferService accountTransferService;

    @PostMapping
    public ResponseEntity<AccountDto> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountDto created = accountTransferService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountDto> getAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountTransferService.getAccountByNumber(accountNumber));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> executeTransfer(@Valid @RequestBody TransferRequest request) {
        TransferResponse response = accountTransferService.transferFunds(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint explicitly designed to test optimistic locking collisions.
     */
    @PostMapping("/{accountNumber}/simulate-concurrency")
    public ResponseEntity<AccountDto> simulateConcurrency(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amountDelta,
            @RequestParam(required = false) Long staleVersion) {
        AccountDto updated = accountTransferService.updateBalanceDirectly(accountNumber, amountDelta, staleVersion);
        return ResponseEntity.ok(updated);
    }
}
