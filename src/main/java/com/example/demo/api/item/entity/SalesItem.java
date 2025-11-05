package com.example.demo.api.item.entity;

import com.example.demo.api.item.enums.SalesItemStatus;
import com.example.demo.api.item.enums.SalesItemType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

/*
* item 이랑 SalesItem 구분 이유.
* 상품 자체는 변하지 않지만 판매 기간이나 가격 인당 재고 변할 수 있음. -> 업데이트 사이클이 다른 거 분리.
* 할인 정책 등이 도입될 경우 -> item salesItem saleItemPrice 등으로 분리 가능.
*
* */

@Getter
@Entity
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class SalesItem {
    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name="sales_item_id")
    private Long id;

    private Long initialQuantity;
    private Long remainingQuantity;

    @Enumerated(STRING)
    private SalesItemStatus salesItemStatus;  // item 하나에 대해 'ON' 두 개 이상 절대 금지.

    @Enumerated(STRING)
    private SalesItemType salesItemType;

    private LocalDateTime salesStartDate;
    private LocalDateTime salesEndDate;

    @ManyToOne(fetch=LAZY)
    @JoinColumn(name="item_id")
    private Item item;
}
