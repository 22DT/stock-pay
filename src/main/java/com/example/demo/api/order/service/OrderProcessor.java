package com.example.demo.api.order.service;

import com.example.demo.api.item.dto.internal.StockDecreaseContext;
import com.example.demo.api.item.entity.SalesItem;
import com.example.demo.api.item.repository.SalesItemRepository;
import com.example.demo.api.item.service.ItemUpdater;
import com.example.demo.api.order.entity.Order;
import com.example.demo.api.order.entity.OrderItem;
import com.example.demo.api.order.repository.OrderItemRepository;
import com.example.demo.api.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderProcessor {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final SalesItemRepository salesItemRepository;
    private final ItemUpdater itemUpdater;


    /**
     * 재고 감소.
     */

    public void processStockOnOrder(String merchantOrderId){

        /*
         * 재고 감소 준비
         * */

        // 주문 조회
        Order order = orderRepository.findByMerchantOrderId(merchantOrderId).get();

        // 주문 아이템 조회
        List<OrderItem> orderItems = orderItemRepository.findByOrderIdWithItem(order.getId());

        // 주문 아이템에 포함된 상품 ID 리스트
        List<Long> itemIds = orderItems.stream()
                .map(orderItem -> orderItem.getItem().getId())
                .toList();

        // key: itemId, value: 주문 수량
        Map<Long, Long> itemIdToOrderQuantity = new HashMap<>();
        orderItems.forEach(orderItem -> {
            itemIdToOrderQuantity.put(orderItem.getItem().getId(), orderItem.getQuantity());
        });

        // 판매중인 SalesItem 조회
        List<SalesItem> salesItems = salesItemRepository.findOnSalesItemsByItemIdsWithItem(itemIds);

        // key: itemId, value: SalesItem 엔티티
        Map<Long, SalesItem> itemIdToSalesItem = new HashMap<>();

        // key: itemId, value: salesItemId (PK)
        Map<Long, Long> itemIdToSalesItemId = new HashMap<>();

        salesItems.forEach(salesItem -> {
            Long itemId = salesItem.getItem().getId();
            itemIdToSalesItem.put(itemId, salesItem);
            itemIdToSalesItemId.put(itemId, salesItem.getId());
        });

        // 실제 재고 감소 처리

        StockDecreaseContext context = StockDecreaseContext.of(order, orderItems, itemIdToSalesItem, itemIdToSalesItemId);

        itemUpdater.decreaseStockForOrder(context);

    }

}
