package com.example.demo.api.pay.exception;

import org.springframework.http.HttpStatus;

public class PaymentUnknownException extends PaymentException {
    public PaymentUnknownException(HttpStatus httpStatus, String message) {
        super(httpStatus, message);
    }
}
