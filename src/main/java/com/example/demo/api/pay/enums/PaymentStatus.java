package com.example.demo.api.pay.enums;

public enum PaymentStatus {
    IN_PROGRESS("IN_PROGRESS"),
    DONE("DONE"),
    CANCELED("CANCELED"),
    ABORTED("ABORTED"),
    EXPIRED("EXPIRED"),
    RETRY("RETRY"),
    DONE_CANCELED("DONE_CANCELED"),
    TIMEOUT("TIMEOUT"),
    UNKNOWN("UNKNOWN")


    ;

    private final String status;

    PaymentStatus(String status) {
        this.status = status;
    }

    public static PaymentStatus fromTossStatus(TossPaymentStatus tossStatus) {
        return switch (tossStatus) {
            case IN_PROGRESS -> IN_PROGRESS;
            case DONE -> DONE;
            case CANCELED -> CANCELED;
            case ABORTED -> ABORTED;
            case EXPIRED -> EXPIRED;
            case RETRY -> RETRY;
            case TIMEOUT -> TIMEOUT;
            default -> ABORTED; // 알 수 없는 건 일단 ABORTED 로
        };
    }
}
