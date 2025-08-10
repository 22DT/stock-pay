package com.example.demo.api.pay.exception.cancel;

import org.springframework.http.HttpStatus;

public class PaymentAlreadyCancelException  extends PaymentCancelException {
    public PaymentAlreadyCancelException(HttpStatus httpStatus, String message) {
        super(httpStatus, message);
    }

    public PaymentAlreadyCancelException(HttpStatus httpStatus, String message, String errorCode) {
        super(httpStatus, message, errorCode);
    }
}
