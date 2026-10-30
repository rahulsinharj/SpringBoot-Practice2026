package com.enterprise.mongodb.web.exception;

public class InvalidTransactionException extends BusinessException {

    public InvalidTransactionException(String message) {
        super(message, "INVALID_TRANSACTION");
    }
}
