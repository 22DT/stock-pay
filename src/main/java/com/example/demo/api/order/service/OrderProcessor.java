package com.example.demo.api.order.service;

import com.example.demo.api.item.dto.internal.StockDecreaseContext;
import com.example.demo.api.item.entity.SalesItem;
import com.example.demo.api.item.entity.StockHistory;
import com.example.demo.api.item.repository.SalesItemRepository;
import com.example.demo.api.item.repository.StockHistoryRepository;
import com.example.demo.api.item.service.ItemUpdater;
import com.example.demo.api.member.entity.Member;
import com.example.demo.api.member.repository.MemberRepository;
import com.example.demo.api.order.entity.Order;
import com.example.demo.api.order.entity.OrderItem;
import com.example.demo.api.order.enums.OrderItemStatus;
import com.example.demo.api.order.enums.OrderStatus;
import com.example.demo.api.order.repository.OrderItemRepository;
import com.example.demo.api.order.repository.OrderRepository;
import com.example.demo.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

        // order 상태 업데이트
        orderRepository.updateStatus(order.getId(), OrderStatus.STOCK_PROCESSED);
    }


    /**
     * 데드락과 lock 지연 해결하기 위해 트랜잭션 범위 감소한 버전
     */
    public void processStockOnOrderV2(Long buyerId, String merchantOrderId) {

        /*
         * 재고 감소 준비
         *
         * 구매할 상품과 얼마나 구입할지 찾아야 함.
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


        salesItems.sort(Comparator.comparing(SalesItem::getId)); // 그렇게 의미는 없을 듯?

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

                // roll back
                for (Long okSalesItemId : okSalesItemIds) {

                    OrderItem orderItem1 = salesItemIdToOrderItem.get(okSalesItemId);
                    Long quantity = orderItem1.getQuantity();

                    SalesItem salesItem1 = salesItemMap.get(okSalesItemId);

                    itemUpdater.rollbackStockPerItem(salesItem1, quantity, buyer, order, orderItem1.getId());
                }


                // 전체 - ok: 이것들은 실패 처리해줘야 함.
                List<Long> salesItemIds = new ArrayList<>(salesItems.stream()
                        .map(SalesItem::getId).toList());

                salesItemIds.removeAll(okSalesItemIds);

                salesItemIds.forEach((salesItemId1) ->{
                    OrderItem orderItem2 = salesItemIdToOrderItem.get(salesItemId1);

                    orderItemRepository.updateStatus(orderItem2.getId(), OrderItemStatus.CANCELLED);
                });

                orderRepository.updateStatus(order.getId(), OrderStatus.FAILED);

                throw e;
            }

        });

        // order 상태 업데이트
        orderRepository.updateStatus(order.getId(), OrderStatus.STOCK_PROCESSED);
    }



    // 결제 실패 시 해당 주문과 관련된 재고 전부 rollback 하는 메소드
    public void stockRollback(Long buyerId, String merchantOrderId){

        Member buyer = memberRepository.findById(buyerId).get();

        // 주문 조회
        Order order = orderRepository.findByMerchantOrderId(merchantOrderId).get();

        // 주문 아이템 조회
        List<OrderItem> orderItems = orderItemRepository.findByOrderIdWithItem(order.getId());
        // key: itemId, value: OrderItem
        Map<Long, OrderItem> itemIdToOrderItem = orderItems.stream()
                .collect(Collectors.toMap(orderItem -> orderItem.getItem().getId(), Function.identity()));


        // StockHistory 갖고 와야 함.
        List<StockHistory> stockHistories = stockHistoryRepository.findByOrderIdAndBuyerIdWithSalesItemAndItem(order.getId(), buyerId);

        stockHistories.stream()
                .filter(stockHistory -> {
                    SalesItem salesItem = stockHistory.getSalesItem();
                    Long itemId = salesItem.getItem().getId();
                    OrderItem orderItem = itemIdToOrderItem.get(itemId);
                    return orderItem.getStatus().equals(OrderItemStatus.SUCCESS);
                })
                .forEach(stockHistory -> {
                    SalesItem salesItem = stockHistory.getSalesItem();
                    Long itemId = salesItem.getItem().getId();
                    OrderItem orderItem = itemIdToOrderItem.get(itemId);

                    Long quantity = stockHistory.getChangeQuantity();
                    itemUpdater.rollbackStockPerItem(salesItem, quantity, buyer, order, orderItem.getId());
                });

    }
}
