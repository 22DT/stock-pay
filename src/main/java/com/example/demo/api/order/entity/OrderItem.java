package com.example.demo.api.order.entity;

import com.example.demo.api.item.entity.Item;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@NoArgsConstructor
@Getter
public class OrderItem {

    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name="order_item_id")
    private Long id;

    private Long quantity;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name="order_id")
    private Order order;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name="item_id")
    private Item item;


    public OrderItem(Long quantity, Order order, Item item) {
        this.quantity = quantity;
        this.order = order;
        this.item = item;
    }
}
