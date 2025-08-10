package com.example.demo.api.pay.entity;

import com.example.demo.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@NoArgsConstructor
@Getter
@Builder
@AllArgsConstructor
public class PaymentCancelHistory extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name="payment_cancel_hisotry_id")
    private Long id;

    @Column(unique = true)
    private String paymentKey;
    @Column(unique = true)
    private String transactionKey;
    private String cancelAmount;
    private String cancelReason;
    private OffsetDateTime canceledAt;
}
