package com.example.demo.api.pay.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class PaymentException extends RuntimeException {

    private HttpStatus httpStatus;
    private String errorCode;

    public PaymentException(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public PaymentException(HttpStatus httpStatus, String message, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}
