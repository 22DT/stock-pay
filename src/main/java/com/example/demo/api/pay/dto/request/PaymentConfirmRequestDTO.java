package com.example.demo.api.pay.dto.request;


public record PaymentConfirmRequestDTO(
        String paymentKey,
        String merchantOrderId,
        String amount
) {
}
