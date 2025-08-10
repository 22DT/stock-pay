package com.example.demo.api.pay.exception.cancel;

import com.example.demo.api.pay.exception.PaymentException;
import org.springframework.http.HttpStatus;

public class PaymentCancelException extends PaymentException {
    public PaymentCancelException(HttpStatus httpStatus, String message) {
        super(httpStatus, message);
    }

    public PaymentCancelException(HttpStatus httpStatus, String message, String errorCode) {
        super(httpStatus, message, errorCode);
    }
}
