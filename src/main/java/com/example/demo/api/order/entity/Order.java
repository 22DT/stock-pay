package com.example.demo.api.order.entity;

import com.example.demo.api.member.entity.Member;
import com.example.demo.api.order.enums.OrderStatus;
import com.example.demo.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

/**
 *
 * merchantOrderId snowflake 고려
 *
 */

@Entity
@Table(name = "orders")
@NoArgsConstructor
@Getter
@Builder
@AllArgsConstructor
public class Order extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name="order_id")
    private Long id;

    @Column(unique=true)
    private String merchantOrderId ;

    private String amount;

    @Enumerated(STRING)
    private OrderStatus status;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name="buyer_id")
    private Member buyer;
}
