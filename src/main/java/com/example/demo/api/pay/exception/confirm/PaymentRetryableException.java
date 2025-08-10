package com.example.demo.api.pay.exception.confirm;

import org.springframework.http.HttpStatus;

public class PaymentRetryableException extends PaymentConfirmException {
    public PaymentRetryableException(HttpStatus httpStatus, String message) {
        super(httpStatus, message);
    }

    public PaymentRetryableException(HttpStatus httpStatus, String message, String errorCode) {
        super(httpStatus, message, errorCode);
    }


}
