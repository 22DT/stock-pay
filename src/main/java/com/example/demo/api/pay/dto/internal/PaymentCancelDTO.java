package com.example.demo.api.pay.dto.internal;


import com.example.demo.api.pay.dto.external.response.TossPaymentResponseDTO;

import java.time.OffsetDateTime;
import java.util.List;

public record PaymentCancelDTO(
        String cancelAmount,
        String cancelReason,
        int taxFreeAmount,
        int taxExemptionAmount,
        int refundableAmount,
        int transferDiscountAmount,
        int easyPayDiscountAmount,
        OffsetDateTime canceledAt,
        String transactionKey,
        String receiptKey,
        String cancelStatus,
        String cancelRequestId
) {
    public static PaymentCancelDTO from(TossPaymentResponseDTO.Cancel cancel) {
        return new PaymentCancelDTO(
                String.valueOf(cancel.cancelAmount()),
                cancel.cancelReason(),
                cancel.taxFreeAmount(),
                cancel.taxExemptionAmount(),
                cancel.refundableAmount(),
                cancel.transferDiscountAmount(),
                cancel.easyPayDiscountAmount(),
                cancel.canceledAt(),
                cancel.transactionKey(),
                cancel.receiptKey(),
                cancel.cancelStatus(),
                cancel.cancelRequestId()
        );
    }

    public static List<PaymentCancelDTO> fromList(List<TossPaymentResponseDTO.Cancel> cancels) {
        if (cancels == null || cancels.isEmpty()) {
            return List.of();
        }
        return cancels.stream()
                .map(PaymentCancelDTO::from)
                .toList();
    }
}

