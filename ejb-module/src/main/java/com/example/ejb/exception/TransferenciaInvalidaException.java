package com.example.ejb.exception;

public class TransferenciaInvalidaException extends RuntimeException {  // ← RuntimeException!

    public TransferenciaInvalidaException(String message) {
        super(message);
    }

    public TransferenciaInvalidaException(String message, Throwable cause) {
        super(message, cause);
    }
}