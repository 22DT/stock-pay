package com.example.demo.api.order.service;

import com.example.demo.api.item.dto.internal.StockDecreaseContext;
import com.example.demo.api.item.entity.SalesItem;
import com.example.demo.api.item.entity.StockHistory;
import com.example.demo.api.item.enums.StockHistoryType;
import com.example.demo.api.item.repository.SalesItemRepository;
import com.example.demo.api.item.repository.StockHistoryRepository;
import com.example.demo.api.item.service.ItemUpdater;
import com.example.demo.api.member.entity.Member;
import com.example.demo.api.member.repository.MemberRepository;
import com.example.demo.api.order.entity.Order;
import com.example.demo.api.order.entity.OrderItem;
import com.example.demo.api.order.repository.OrderItemRepository;
import com.example.demo.api.order.repository.OrderRepository;
import com.example.demo.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderProcessor {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final SalesItemRepository salesItemRepository;
    private final ItemUpdater itemUpdater;
    private final MemberRepository memberRepository;
    private final StockHistoryRepository stockHistoryRepository;


    /**
     * 재고 감소.
     */

    public void processStockOnOrder(Long buyerId, String merchantOrderId){

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


        // 판매중인 SalesItem 조회
        List<SalesItem> salesItems = salesItemRepository.findOnSalesItemsByItemIdsWithItem(itemIds);

        // key: itemId value: orderItem
        Map<Long, OrderItem> itemIdToOrderItem = orderItems.stream()
                .collect(Collectors.toMap(orderItem -> orderItem.getItem().getId(), Function.identity()));

        // key: salesItemId, value: OrderItem
        Map<Long, OrderItem> salesItemIdToOrderItem = salesItems.stream()
                .collect(Collectors.toMap(
                        SalesItem::getId,
                        salesItem -> itemIdToOrderItem.get(salesItem.getItem().getId())
                ));

        // 실제 재고 감소 처리

        StockDecreaseContext context = StockDecreaseContext.of(order, salesItems, salesItemIdToOrderItem);

        itemUpdater.decreaseStockForOrder(buyerId, context);
    }


    /**
     * 데드락과 lock 지연 해결하기 위해 트랜잭션 범위 감소한 버전
     */
    public void processStockOnOrderV2(Long buyerId, String merchantOrderId) {

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


        // 판매중인 SalesItem 조회
        List<SalesItem> salesItems = salesItemRepository.findOnSalesItemsByItemIdsWithItem(itemIds);
        // key: salesItemId value: salesItem
        Map<Long, SalesItem> salesItemMap = salesItems.stream()
                .collect(Collectors.toMap(SalesItem::getId, Function.identity()));

        // key: itemId, value: OrderItem
        Map<Long, OrderItem> itemIdToOrderItem = orderItems.stream()
                .collect(Collectors.toMap(orderItem -> orderItem.getItem().getId(), Function.identity()));

        // key: salesItemId, value: OrderItem
        Map<Long, OrderItem> salesItemIdToOrderItem = salesItems.stream()
                .collect(Collectors.toMap(
                        SalesItem::getId,
                        salesItem -> itemIdToOrderItem.get(salesItem.getItem().getId())
                ));


        // 실제 재고 감소 처리

        Member buyer = memberRepository.findById(buyerId).get();
        List<Long> okSalesItemIds = new ArrayList<>();


        salesItems.sort(Comparator.comparing(SalesItem::getId));

        salesItems.forEach(salesItem -> {
            Long salesItemId = salesItem.getId();
            OrderItem orderItem = salesItemIdToOrderItem.get(salesItemId);


            try {
                // 재고 감소
                itemUpdater.decreaseStockForOrderPerItem(order, salesItem, orderItem, buyer);

                // 성공한 거 기록
                okSalesItemIds.add(salesItemId);

            } catch (BadRequestException e) { // 이거 재고 에러로 바꾸자.
                //okSalesItemIds rollback 해야 함. 1) 전체 재고 2) 개인 재고

                for (Long okSalesItemId : okSalesItemIds) {

                    OrderItem orderItem1 = salesItemIdToOrderItem.get(okSalesItemId);
                    Long quantity = orderItem1.getQuantity();


                    /*
                    * 트랜잭션 시작
                    * */

                    // 1) 전체 재고  증가.
                    salesItemRepository.incrementStock(okSalesItemId, quantity);

                    // 2) 개인 재고 감소
                    SalesItem salesItem1 = salesItemMap.get(okSalesItemId);

                    StockHistory stockHistory = StockHistory.builder()
                            .changeQuantity(quantity)
                            .stockHistoryType(StockHistoryType.MINUS)
                            .message("다른 상품 실패로 인해 개인 재고 복구")
                            .buyer(buyer)
                            .salesItem(salesItem1)
                            .order(order)
                            .build();

                    stockHistoryRepository.save(stockHistory);

                    /*
                     * 트랜잭션 끝
                     * */
                }
            }

        });

    }
}
