package com.example.demo.api.pay.enums;

import com.example.demo.api.pay.exception.PaymentException;
import com.example.demo.api.pay.exception.confirm.*;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * 외부 예외 우리 예외로 추상화.
 * 1) mapping table 
 * 2) 찾기
 * 3) 생성
 */

public enum TossPaymentConfirmExceptionType {

    ALREADY_DONE(PaymentAlreadyDoneException::new, Set.of(
                    "ALREADY_PROCESSED_PAYMENT"
    )),

    ABORTED(PaymentAbortedException::new, Collections.emptySet()),

    EXPIRED(PaymentExpiredException::new, Set.of(
            "NOT_FOUND_PAYMENT_SESSION"
    ))

    ;

    private final BiFunction<HttpStatus, String, PaymentConfirmException> exceptionFactory;
    private final Set<String> errorCodes;

    TossPaymentConfirmExceptionType(BiFunction<HttpStatus, String, PaymentConfirmException> exceptionFactory,
                                    Set<String> errorCodes) {
        this.exceptionFactory = exceptionFactory;
        this.errorCodes = errorCodes;
    }

    public static PaymentException fromErrorCode(HttpStatus httpStatus, String errorCode, String message) {
        for (TossPaymentConfirmExceptionType type : values()) {
            if (type.errorCodes.contains(errorCode)) {
                return type.exceptionFactory.apply(httpStatus, message);
            }
        }

        return new PaymentAbortedException(httpStatus, message);
    }
}
