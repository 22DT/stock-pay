package com.example.demo.api.order.entity;

import com.example.demo.api.item.entity.Item;
import com.example.demo.api.item.entity.SalesItem;
import com.example.demo.api.order.enums.OrderItemStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@NoArgsConstructor
@Getter
@Builder
@AllArgsConstructor
public class OrderItem {

    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name="order_item_id")
    private Long id;

    private Long quantity;

    @Enumerated(STRING)
    private OrderItemStatus status;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name="order_id")
    private Order order;

    @ManyToOne(fetch = LAZY )
    @JoinColumn(name="sales_item_id")
    private SalesItem salesItem;  // 갑자기 판매 item 이 바뀌면 가격 등 변경이 생길 수 있음?

}
