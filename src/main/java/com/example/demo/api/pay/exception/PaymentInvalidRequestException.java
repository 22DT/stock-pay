package com.example.demo.api.pay.exception;

import org.springframework.http.HttpStatus;

public class PaymentInvalidRequestException extends PaymentException {
    public PaymentInvalidRequestException(HttpStatus httpStatus, String message) {
        super(httpStatus, message);
    }
}