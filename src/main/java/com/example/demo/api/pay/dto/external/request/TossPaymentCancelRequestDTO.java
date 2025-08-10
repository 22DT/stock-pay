package com.example.demo.api.pay.dto.external.request;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TossPaymentCancelRequestDTO(
        String cancelReason,
        Long cancelAmount
) {}
