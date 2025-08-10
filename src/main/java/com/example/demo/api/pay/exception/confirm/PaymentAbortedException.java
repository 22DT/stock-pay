package com.example.demo.api.pay.exception.confirm;

import org.springframework.http.HttpStatus;

public class PaymentAbortedException extends PaymentConfirmException {
    public PaymentAbortedException(HttpStatus httpStatus, String message) {
        super(httpStatus, message);
    }

    public PaymentAbortedException(HttpStatus httpStatus, String message, String errorCode) {
        super(httpStatus, message, errorCode);
    }
}
