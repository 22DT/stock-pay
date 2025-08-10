package com.example.demo.api.pay.entity;


import com.example.demo.api.pay.enums.PaymentStatus;
import com.example.demo.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.EnumType.STRING;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PaymentReconcileHistory extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="payment_reconcile_history_id")
    private Long id;

    private String paymentsKey;

    @Enumerated(STRING)
    private PaymentStatus prevStatus;
    @Enumerated(STRING)
    private PaymentStatus newStatus;
    private String reconcileReason;
    private String result; // SUCCESS, FAILURE
    @Column(columnDefinition = "TEXT")
    private String message; // 상세 로그 (API 응답, 오류 등)


    /**
     * 후보정 성공 시 히스토리 생성
     */
    public static PaymentReconcileHistory success(String paymentsKey, PaymentStatus prevStatus, PaymentStatus newStatus, String reconcileReason, String message
    ) {
        return PaymentReconcileHistory.builder()
                .paymentsKey(paymentsKey)
                .prevStatus(prevStatus)
                .newStatus(newStatus)
                .reconcileReason(reconcileReason)
                .result("SUCCESS")
                .message(message)
                .build();
    }

    /**
     * 후보정 실패 시 히스토리 생성
     */
    public static PaymentReconcileHistory failure(String paymentsKey, PaymentStatus prevStatus, String reconcileReason, String message
    ) {
        return PaymentReconcileHistory.builder()
                .paymentsKey(paymentsKey)
                .prevStatus(prevStatus)
                .newStatus(prevStatus) // 실패니까 상태는 그대로
                .reconcileReason(reconcileReason)
                .result("FAILURE")
                .message(message)
                .build();
    }
}
