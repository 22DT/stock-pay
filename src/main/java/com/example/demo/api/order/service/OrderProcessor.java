package com.example.demo.api.order.service;

import com.example.demo.api.item.entity.SalesItem;
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
import java.util.Collections;
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
    private final ItemUpdater itemUpdater;
    private final MemberRepository memberRepository;
    private final StockHistoryRepository stockHistoryRepository;


    /**
     * 재고 감소.
     */

    public void processStockOnOrder(Long buyerId, String merchantOrderId){

        /*
         * 재고 감소 준비
         *
         * 구매할 상품과 얼마나 구입할지 찾아야 함.
         * */

        // 주문 조회
        Order order = orderRepository.findByMerchantOrderId(merchantOrderId).get();

        // 주문 아이템 조회
        List<OrderItem> orderItems = orderItemRepository.findByOrderIdWithSalesItemAndItem(order.getId());

        // 데드락 확인할려고 섞는다.(데스트용임)
        Collections.shuffle(orderItems);

        try {
            // 재고 감소
            itemUpdater.decreaseStockForOrder(buyerId, order, orderItems);

        }catch (BadRequestException e){

            List<Long> orderItemIds = orderItems.stream()
                    .map(OrderItem::getId)
                    .toList();

            orderItemRepository.updateStatus(orderItemIds, OrderItemStatus.ROLLBACK_DONE);

            orderRepository.updateStatus(order.getId(), OrderStatus.FAILED);

            throw e;
        }
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
        List<OrderItem> orderItems = orderItemRepository.findByOrderIdWithSalesItemAndItem(order.getId());

        // 데드락 확인할려고 섞는다.(데스트용임)
        Collections.shuffle(orderItems);

        // 실제 재고 감소 처리

        Member buyer = memberRepository.findById(buyerId).get();
        List<OrderItem> okOrderItems = new ArrayList<>();


        for(int i=0;i<orderItems.size();i++){
            OrderItem orderItem = orderItems.get(i);

            try {
                // 재고 감소
                itemUpdater.decreaseStockForOrderPerItem(orderItem, buyer);

                // 성공한 거 기록
                okOrderItems.add(orderItem);

            } catch (BadRequestException e) { // 이거 재고 에러로 바꾸자.
                //okSalesItemIds rollback 해야 함. 1) 전체 재고 2) 개인 재고

                // roll back
                for (OrderItem orderItem1 : okOrderItems) {

                    Long quantity = orderItem1.getQuantity();

                    itemUpdater.rollbackStockPerItem(orderItem1.getSalesItem(), quantity, buyer, orderItem1.getId());
                }

                // 나머지 실패 처리해줘야 함.
                for(int j=i;j<orderItems.size();j++){

                    orderItemRepository.updateStatus(orderItems.get(j).getId(), OrderItemStatus.CANCELLED);
                }
                orderRepository.updateStatus(order.getId(), OrderStatus.FAILED);

                throw e;
            }

        }

        // order 상태 업데이트
        orderRepository.updateStatus(order.getId(), OrderStatus.STOCK_PROCESSED);

    }




    // 결제 실패 시 해당 주문과 관련된 재고 전부 rollback 하는 메소드
    public void stockRollback(Long buyerId, String merchantOrderId){

        Member buyer = memberRepository.findById(buyerId).get();

        // 주문 조회
        Order order = orderRepository.findByMerchantOrderId(merchantOrderId).get();

        // 주문 아이템 조회
        List<OrderItem> orderItems = orderItemRepository.findByOrderIdWithSalesItem(order.getId());

        orderItems.stream()
                .filter(orderItem -> orderItem.getStatus().equals(OrderItemStatus.SUCCESS))
                .forEach(orderItem -> {
                    SalesItem salesItem = orderItem.getSalesItem();
                    Long quantity = orderItem.getQuantity();
                    itemUpdater.rollbackStockPerItem(salesItem, quantity, buyer, orderItem.getId());
                });

    }
}
