package com.example.demo.api.pay.exception.confirm;

import com.example.demo.api.pay.exception.PaymentException;
import org.springframework.http.HttpStatus;

public class PaymentConfirmException extends PaymentException {
    public PaymentConfirmException(HttpStatus httpStatus, String message) {
        super(httpStatus, message);
    }

    public PaymentConfirmException(HttpStatus httpStatus, String message, String errorCode) {
        super(httpStatus, message, errorCode);
    }
}
