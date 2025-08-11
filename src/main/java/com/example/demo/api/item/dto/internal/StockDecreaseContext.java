package com.example.demo.api.item.dto.internal;

import com.example.demo.api.item.entity.SalesItem;
import com.example.demo.api.order.entity.Order;
import com.example.demo.api.order.entity.OrderItem;

import java.util.List;
import java.util.Map;

public record StockDecreaseContext(
        Order order,
        List<OrderItem> orderItems,
        Map<Long, SalesItem> itemIdToSalesItem,
        Map<Long, Long> itemIdToSalesItemId
) {
    public static StockDecreaseContext of(Order order,
                                          List<OrderItem> orderItems,
                                          Map<Long, SalesItem> itemIdToSalesItem,
                                          Map<Long, Long> itemIdToSalesItemId) {
        return new StockDecreaseContext(order, orderItems, itemIdToSalesItem, itemIdToSalesItemId);
    }
}

