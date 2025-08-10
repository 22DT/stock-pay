package com.example.demo.api.item.service;

import com.example.demo.api.item.entity.SalesItem;
import com.example.demo.api.item.repository.ItemRepository;
import com.example.demo.api.item.repository.SalesItemRepository;
import com.example.demo.api.order.entity.Order;
import com.example.demo.api.order.entity.OrderItem;
import com.example.demo.api.order.repository.OrderItemRepository;
import com.example.demo.api.order.repository.OrderRepository;
import com.example.demo.common.exception.BadRequestException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ItemUpdater {
    private final ItemRepository itemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final SalesItemRepository salesItemRepository;


    /**
     * 재고 감소.
     */

    public void processStockOnOrder(String merchantOrderId){
        // order 조회
        Order order = orderRepository.findByMerchantOrderId(merchantOrderId).get();

        // orderItem 조회
        List<OrderItem> orderItems = orderItemRepository.findByOrderIdWithItem(order.getId());
        List<Long> itemIds = orderItems.stream()
                .map(orderItem -> orderItem.getItem().getId())
                .toList();

        // key: itemId, value: 구입 수량
        Map<Long, Long> map1 = new HashMap<>();
        orderItems.forEach(orderItem -> {
            map1.put(orderItem.getOrder().getId(), orderItem.getQuantity());
        });


        // 각 item 에 대한 on salesItem 조회
        List<SalesItem> salesItems = salesItemRepository.findOnSalesItemsByItemIdsWithItem(itemIds);


        // key: itemId, value: (totalQuantity, perLimitQuantity)
        Map<Long, StockLimit> map2 = new HashMap<>();
        // key: itemId, value: salesItemId
        Map<Long, Long> map3 = new HashMap<>();

        salesItems.forEach(salesItem -> {
            Long itemId = salesItem.getItem().getId();
            StockLimit stockLimit = new StockLimit(salesItem.getTotalQuantity(), salesItem.getPerLimitQuantity());
            map2.put(itemId, stockLimit);
            map3.put(itemId, salesItem.getId());
        });


        // 재고 감소

        orderItems.forEach(orderItem -> {

            /*
             * 트랜잭션 시작
             * */

            Long itemId = orderItem.getItem().getId();
            Long requestStock = orderItem.getQuantity();

            // 전체 재고
            Long salesItemId = map3.get(itemId);
            Long affectedRows = salesItemRepository.decreaseStock(salesItemId, requestStock);

            if (affectedRows == 0) {
                throw new BadRequestException("재고 부족");
            }

            // 인당 재고

            // history insert


            /*
             * 트랜재션 종료
             * */


        });

    }


    @Getter
    @AllArgsConstructor
    class StockLimit {
        Long totalQuantity;
        Long perLimitQuantity;
    }

}
