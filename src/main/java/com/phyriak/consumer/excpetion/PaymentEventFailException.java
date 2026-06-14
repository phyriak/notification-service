package com.phyriak.consumer.excpetion;

public class PaymentEventFailException extends RuntimeException{

    public PaymentEventFailException(String message, Throwable cause) {
        super(message, cause);
    }
}
