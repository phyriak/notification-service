package com.phyriak.payment_consumer.exception;

public class PaymentEventException extends RuntimeException{

    public PaymentEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
