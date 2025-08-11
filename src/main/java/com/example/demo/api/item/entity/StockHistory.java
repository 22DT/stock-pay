package com.example.demo.api.item.entity;

import com.example.demo.api.item.enums.StockHistoryType;
import com.example.demo.api.order.entity.Order;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

@Getter
@Entity
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class StockHistory {
    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name="stock_history_id")
    private Long id;

    private Long changeQuantity;
    @Enumerated(STRING)
    private StockHistoryType stockHistoryType;
    private String message; // 상품 구매 or 환불 or 결제 실패 시 다시 증가 등

    @ManyToOne(fetch=LAZY)
    @JoinColumn(name="sales_item_id")
    private SalesItem salesItem;

    @ManyToOne(fetch=LAZY)
    @JoinColumn(name="order_id")
    private Order order ;
}
