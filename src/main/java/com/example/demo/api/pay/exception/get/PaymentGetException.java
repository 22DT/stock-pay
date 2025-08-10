package com.example.demo.api.pay.exception.get;

import com.example.demo.api.pay.exception.PaymentException;
import org.springframework.http.HttpStatus;

public class PaymentGetException extends PaymentException {
    public PaymentGetException(HttpStatus httpStatus, String message) {
        super(httpStatus, message);
    }

    public PaymentGetException(HttpStatus httpStatus, String message, String errorCode) {
        super(httpStatus, message, errorCode);
    }
}
