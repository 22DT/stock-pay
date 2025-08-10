package com.example.demo.api.pay.exception.confirm;

import org.springframework.http.HttpStatus;

public class PaymentAlreadyDoneException extends PaymentConfirmException {
    public PaymentAlreadyDoneException(HttpStatus httpStatus, String message) {
        super(httpStatus, message);
    }

    public PaymentAlreadyDoneException(HttpStatus httpStatus, String message, String errorCode) {
        super(httpStatus, message, errorCode);
    }
}
