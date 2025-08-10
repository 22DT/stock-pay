package com.example.demo.api.pay.enums;

public enum TossPaymentStatus {

    /**
     * 토스
     */

    READY("READY"),
    IN_PROGRESS("IN_PROGRESS"),
    WAITING_FOR_DEPOSIT("WAITING_FOR_DEPOSIT"),
    DONE("DONE"),
    CANCELED("CANCELED"),
    PARTIAL_CANCELED("PARTIAL_CANCELED"),
    ABORTED("ABORTED"),
    EXPIRED("EXPIRED"),

    /**
     * 우리가 필요한 거.
     */

    ALREADY_DONE("ALREADY_DONE"),
    ALREADY_CANCELED("ALREADY_CANCELED"),
    RETRY("RETRY"),
    TIMEOUT("TIMEOUT"),
    UNKNOWN("UNKNOWN")
    ;

    private final String status;

    TossPaymentStatus(String status) {
        this.status = status;
    }

    public static TossPaymentStatus fromStatus(String status) {
        for (TossPaymentStatus tossPaymentStatus : values()) {
            if (tossPaymentStatus.status.equalsIgnoreCase(status)) {
                return tossPaymentStatus;
            }
        }
        return UNKNOWN; // 또는 원하는 기본 상태
    }
}

