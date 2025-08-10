package com.example.demo.api.pay.dto.external.request;

public record TossPaymentConfirmRequestDTO(
        String paymentKey,
        String orderId,
        Long amount
) {
}
