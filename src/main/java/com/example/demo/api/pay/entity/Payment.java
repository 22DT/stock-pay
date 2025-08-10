package com.example.demo.api.pay.entity;

import com.example.demo.api.order.entity.Order;
import com.example.demo.api.pay.dto.internal.PaymentDTO;
import com.example.demo.api.pay.enums.PaymentStatus;
import com.example.demo.common.entity.ExtendedBaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(
        name = "payment",
        indexes = {
                @Index(name = "idx_payment_status_created_date", columnList = "paymentStatus, createdDate")
                /*
                 * IN_PROGRESS, TIMEOUT 비율이 크다면 인덱스 안 태울 수 있음. (이 비율은 전체 데이터 크기가 클수록 감소.)
                 * 비율이 작다해도 리프 노드가 멀리 떨어져 있을 경우 필터링 효율 낮을 수 있음.
                 * -> 각각 조회해서 union 하는 방법 고려.
                 */
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
public class Payment extends ExtendedBaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @Column(unique = true)
    private String paymentKey; // 토스 결제 고유 키

    @Enumerated(STRING)
    private PaymentStatus paymentStatus;

    private String totalAmount;
    private String orderName;                    // 주문명 (상품 또는 서비스 이름)
    private String method;                       // 결제 수단 (예: CARD, TRANSFER, MOBILE_PHONE 등)
    private OffsetDateTime approvedAt;           // 결제 승인 시각 (승인 완료 시점)

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    public void updatePayment(PaymentDTO paymentDTO){
        paymentStatus =PaymentStatus.fromTossStatus(paymentDTO.tossPaymentStatus());
        totalAmount= paymentDTO.totalAmount();
        orderName= paymentDTO.orderName();
        method= paymentDTO.method();
        approvedAt= paymentDTO.approvedAt();
    }


    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
    }
}
