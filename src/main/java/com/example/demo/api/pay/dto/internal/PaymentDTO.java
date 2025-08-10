package com.example.demo.api.pay.dto.internal;


import com.example.demo.api.pay.dto.external.response.TossPaymentResponseDTO;
import com.example.demo.api.pay.enums.TossPaymentStatus;

import java.time.OffsetDateTime;

public record PaymentDTO(
        TossPaymentStatus tossPaymentStatus,
        String paymentKey,
        String totalAmount,
        String method,
        OffsetDateTime approvedAt,
        String orderName
) {

    public static PaymentDTO from(TossPaymentResponseDTO response) {
        return new PaymentDTO(
                TossPaymentStatus.fromStatus(response.status()),
                response.paymentKey(),
                String.valueOf(response.totalAmount()),
                response.method(),
                response.approvedAt(),
                response.orderName()
        );
    }

}



