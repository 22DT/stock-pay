package com.example.demo.api.item.service;

import com.example.demo.api.item.dto.internal.StockDecreaseContext;
import com.example.demo.api.item.entity.SalesItem;
import com.example.demo.api.item.entity.StockHistory;
import com.example.demo.api.item.enums.StockHistoryType;
import com.example.demo.api.item.repository.SalesItemRepository;
import com.example.demo.api.item.repository.StockHistoryRepository;
import com.example.demo.api.member.entity.Member;
import com.example.demo.api.member.repository.MemberRepository;
import com.example.demo.api.order.entity.Order;
import com.example.demo.api.order.entity.OrderItem;
import com.example.demo.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ItemUpdater {
    private final SalesItemRepository salesItemRepository;
    private final StockHistoryRepository stockHistoryRepository;
    private final MemberRepository memberRepository;

    /**
     * 실제 재고 감소
     * 1) 전체 재고 감소
     * 2) 인당 재고 감소
     *
     * 문제점: 1. 전체 재고 감소 시 데드락 2. 락으로 인해 대기 시간 증가.
     *
     * 1. 전체 재고 감소 시 데드락
     * 원인: 하나의 트랙잭션 내에서 여러 lock 이 필요
     * 해결:
     * 1) salesItemId 정렬해 데드락 방지
     * 그래도 혹시 모르니 실제 데드락 발생 시
     * 2) dead lock 감지 스레드
     * 각 트랜잭션이 요구하는 잠금의 수 증가 시 db 부하 증가
     * 3) dead lock 감지 스레드 끄고 lock 획득 timeout
     * 4) 트랜잭션 범위 줄인다. 전: 상품들 후 : 각 상품 -> 하나의 트랜잭션 내 하나의 lock 만 유지
     * 데드락 완전 x but 중간에 예외 발생 시 rollback 로직 필요. 복잡성 증가.
     *
     *
     * 2. 락으로 인해 대기 시간 증가.
     * 원인: update lock 은 commit 후 반환됨.
     * 해결:
     * 1) 이것도 트랜잭션의 범위를 줄인다.

     *
     * -> 최종 결론은 트랜잭션의 범위를 줄인다. 복잡성이 증가하지만 두 문제 해결할 수 있음.?
     */

    @Transactional
    public void decreaseStockForOrder(Long buyerId, StockDecreaseContext context) {
        Member buyer = memberRepository.findById(buyerId).get();

        Order order = context.order();
        List<SalesItem> salesItems = context.salesItems();
        Map<Long, OrderItem> salesItemIdToOrderItem = context.salesItemIdToOrderItem();

        salesItems.sort(Comparator.comparing(SalesItem::getId));


        salesItems.forEach(salesItem -> {
            Long salesItemId = salesItem.getId();

            OrderItem orderItem = salesItemIdToOrderItem.get(salesItemId);
            Long requestStock = orderItem.getQuantity();

            // 전체 재고
            Long affectedRows = salesItemRepository.decreaseStock(salesItemId, requestStock);
            if (affectedRows == 0) {throw new BadRequestException("재고 부족");}

            // 인당 재고

            Long currentPurchaseCount = stockHistoryRepository.getCurrentPurchaseCount(buyerId, salesItemId);

            Long perLimitQuantity = salesItem.getPerLimitQuantity();

            if (currentPurchaseCount + requestStock > perLimitQuantity) {
                throw new BadRequestException("인당 구매 개수 초과");
            }

            // history insert

            StockHistory stockHistory = StockHistory.builder()
                    .changeQuantity(requestStock)
                    .stockHistoryType(StockHistoryType.PLUS)
                    .message("상품 구입")
                    .buyer(buyer)
                    .salesItem(salesItem)
                    .order(order)
                    .build();

            stockHistoryRepository.save(stockHistory);
        });
    }


    @Transactional
    public void decreaseStockForOrderPerItem(Order order,SalesItem salesItem, OrderItem orderItem, Member buyer){
        Long salesItemId = salesItem.getId();

        Long requestStock = orderItem.getQuantity();

        // 전체 재고
        Long affectedRows = salesItemRepository.decreaseStock(salesItemId, requestStock);
        if (affectedRows == 0) {throw new BadRequestException("재고 부족");}

        // 인당 재고

        Long currentPurchaseCount = stockHistoryRepository.getCurrentPurchaseCount(buyer.getId(), salesItemId);

        Long perLimitQuantity = salesItem.getPerLimitQuantity();

        if (currentPurchaseCount + requestStock > perLimitQuantity) {
            throw new BadRequestException("인당 구매 개수 초과");
        }

        // history insert

        StockHistory stockHistory = StockHistory.builder()
                .changeQuantity(requestStock)
                .stockHistoryType(StockHistoryType.PLUS)
                .message("상품 구입")
                .buyer(buyer)
                .salesItem(salesItem)
                .order(order)
                .build();

        stockHistoryRepository.save(stockHistory);

    }
}
