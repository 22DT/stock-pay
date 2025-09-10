package com.example.demo.api.item.entity;

import com.example.demo.api.item.enums.StockHistoryType;
import com.example.demo.api.member.entity.Member;
import com.example.demo.api.order.entity.Order;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;


/**
 * 유저 한 명이 여러 기기 등 다수의 세션을 접근 시 동시성 문제 있음 -> 간단한 로컬 캐시로 해결할 예정임,
 * 초기 insert 이후 update 방식이 제일 좋을 듯 싶음.
 */

@Getter
@Entity
@NoArgsConstructor
@Builder
@AllArgsConstructor
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uc_buyer_sales_item", columnNames = {"buyer_id", "sales_item_id"})
        }
)
public class MemberItemStock {
    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name="member_item_stock_id")
    private Long id;

    private Long remainingQuantity;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "buyer_id")
    private Member buyer;

    @ManyToOne(fetch=LAZY)
    @JoinColumn(name="sales_item_id")
    private SalesItem salesItem;

    @ManyToOne(fetch=LAZY)
    @JoinColumn(name="order_id")
    private Order order ;
}
