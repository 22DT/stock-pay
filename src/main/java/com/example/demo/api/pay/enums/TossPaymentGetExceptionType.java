package com.example.demo.api.pay.enums;

import com.example.demo.api.pay.exception.PaymentException;
import com.example.demo.api.pay.exception.get.PaymentGetException;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiFunction;

public enum TossPaymentGetExceptionType {

    DEFAULT(PaymentGetException::new, Collections.emptySet())

    ;

    private final BiFunction<HttpStatus, String, PaymentGetException> exceptionFactory;
    private final Set<String> errorCodes;

    TossPaymentGetExceptionType(BiFunction<HttpStatus, String, PaymentGetException> exceptionFactory,
                                    Set<String> errorCodes) {
        this.exceptionFactory = exceptionFactory;
        this.errorCodes = errorCodes;
    }

    public static PaymentException fromErrorCode(HttpStatus httpStatus, String errorCode, String message) {
        for (TossPaymentGetExceptionType type : values()) {
            if (type.errorCodes.contains(errorCode)) {
                return type.exceptionFactory.apply(httpStatus, message);
            }
        }
        return new PaymentGetException(httpStatus, message);
    }
}
