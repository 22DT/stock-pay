package com.example.demo.api.pay.enums;

import com.example.demo.api.pay.exception.PaymentException;
import com.example.demo.api.pay.exception.cancel.PaymentAlreadyCancelException;
import com.example.demo.api.pay.exception.cancel.PaymentCancelException;
import org.springframework.http.HttpStatus;

import java.util.Set;
import java.util.function.BiFunction;

public enum TossPaymentCancelExceptionType {
    ALREADY_CANCELED(PaymentAlreadyCancelException::new, Set.of(
            "ALREADY_CANCELED_PAYMENT"
    ))

    ;

    private final BiFunction<HttpStatus, String, PaymentCancelException> exceptionFactory;
    private final Set<String> errorCodes;

    TossPaymentCancelExceptionType(BiFunction<HttpStatus, String, PaymentCancelException> exceptionFactory,
                                Set<String> errorCodes) {
        this.exceptionFactory = exceptionFactory;
        this.errorCodes = errorCodes;
    }

    public static PaymentException fromErrorCode(HttpStatus httpStatus, String errorCode, String message) {
        for (TossPaymentCancelExceptionType type : values()) {
            if (type.errorCodes.contains(errorCode)) {
                return type.exceptionFactory.apply(httpStatus, message);
            }
        }
        return new PaymentCancelException(httpStatus, message);
    }
}
