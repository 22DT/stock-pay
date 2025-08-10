package com.example.demo.api.order.dto.query;

import java.time.OffsetDateTime;

public record PassArchivePreviewIdAndPaymentApprovedAtQueryDTO(
        Long passArchiveId,
        OffsetDateTime approvedAt
) {
}
