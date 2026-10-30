package com.enterprise.mongodb.web.exception;

public class InsufficientFundsException extends BusinessException {

    public InsufficientFundsException(String accountNumber, String required, String current) {
        super(String.format("Account '%s' has insufficient funds. Required: %s, Current balance: %s",
                accountNumber, required, current), "INSUFFICIENT_FUNDS");
    }
}
