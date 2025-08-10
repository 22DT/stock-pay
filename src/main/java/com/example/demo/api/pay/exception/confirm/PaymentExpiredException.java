package com.example.demo.api.pay.exception.confirm;

import org.springframework.http.HttpStatus;

public class PaymentExpiredException extends PaymentConfirmException {
    public PaymentExpiredException(HttpStatus httpStatus, String message) {
        super(httpStatus, message);
    }

    public PaymentExpiredException(HttpStatus httpStatus, String message, String errorCode) {
        super(httpStatus, message, errorCode);
    }
}
