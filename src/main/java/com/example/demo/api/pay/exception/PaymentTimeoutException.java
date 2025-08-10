package com.example.demo.api.pay.exception;

import org.springframework.http.HttpStatus;

public class PaymentTimeoutException extends PaymentException {
    public PaymentTimeoutException(HttpStatus httpStatus, String message) {
        super(httpStatus, message);
    }
}
