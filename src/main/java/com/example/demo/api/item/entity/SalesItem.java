package com.example.demo.api.item.entity;

import com.example.demo.api.item.enums.SalesItemStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

@Getter
@Entity
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class SalesItem {
    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name="sales_item_id")
    private Long id;

    private Long totalQuantity;
    private Long perLimitQuantity;

    @Enumerated(STRING)
    private SalesItemStatus salesItemStatus;  // item 하나에 대해 'ON' 두 개 이상 절대 금지.

    private LocalDateTime salesStartDate;
    private LocalDateTime salesEndDate;

    @ManyToOne(fetch=LAZY)
    @JoinColumn(name="item_id")
    private Item item;
}
