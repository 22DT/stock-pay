package com.example.demo.api.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@NoArgsConstructor
@Getter
@Builder
@AllArgsConstructor
public class RefundHistory {
    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name="refund_history_id")
    private Long id;

    private Long quantity;
    private String price;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name="order_item_id")
    private OrderItem orderItem;

}
