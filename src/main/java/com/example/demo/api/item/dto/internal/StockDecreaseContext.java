package com.example.demo.api.item.dto.internal;

import com.example.demo.api.item.entity.SalesItem;
import com.example.demo.api.order.entity.Order;
import com.example.demo.api.order.entity.OrderItem;

import java.util.List;
import java.util.Map;

public record StockDecreaseContext(
        Order order,
        List<SalesItem> salesItems,
        Map<Long, OrderItem> salesItemIdToOrderItem
) {
    public static StockDecreaseContext of(Order order,
                                          List<SalesItem> orderItems,
                                          Map<Long, OrderItem> salesItemIdToOrderItem) {
        return new StockDecreaseContext(order, orderItems, salesItemIdToOrderItem);
    }
}

