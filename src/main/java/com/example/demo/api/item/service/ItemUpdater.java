package com.example.demo.api.item.service;

import com.example.demo.api.item.entity.SalesItem;
import com.example.demo.api.item.entity.StockHistory;
import com.example.demo.api.item.enums.StockHistoryType;
import com.example.demo.api.item.repository.SalesItemRepository;
import com.example.demo.api.item.repository.StockHistoryRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 실제 재고 감소
 * 1) 전체 재고 감소
 * 2) 인당 재고 감소
 *
 * 1. 트랜잭션 범위: 모든 상품
 *
 * 문제점: 1)전체 재고 감소 시 데드락. 2)락으로 인해 대기 시간 증가.
 *
 * 1)전체 재고 감소 시 데드락.
 * 원인: 하나의 트랙잭션 내에서 여러 lock 이 필요
 * 해결:
 * 1-1) salesItemId 정렬해 데드락 방지
 * 그래도 혹시 모르니 실제 데드락 발생 시
 * 1-2) dead lock 감지 스레드
 * 각 트랜잭션이 요구하는 잠금의 수 증가 시 db 부하 증가
 * 1-3) dead lock 감지 스레드 끄고 lock 획득 timeout
 * 1-4) 트랜잭션 범위 줄인다. 전: 상품들 후: 각 상품 -> 하나의 트랜잭션 내 하나의 lock 만 유지
 * 데드락 완전 x but 중간에 예외 발생 시 rollback 로직 필요. 복잡성 증가.
 *
 *
 * 2) 락으로 인해 대기 시간 증가.
 * 원인: update lock 은 commit 후 반환됨.
 * 해결:
 * 2-1) 이것도 트랜잭션의 범위를 줄인다.
 *
 * -> 최종 결론은 트랜잭션의 범위를 줄인다. 전체 상품 -> 각 상품. 복잡성이 증가하지만 두 문제 해결할 수 있음.
 *
 * 2. 트랜잭션 범위: 각 상품
 * 문제점:
 * 1) 중간 상품 실패 시 이전 성공 재고 rollback 해야 함.
 * 이떄, 성공 재고는 서버 메모리에 저장됨. 서버 다운 시 rollback 대상 사라진다.
 *
 * 1)
 * 원인:
 * 해결: 영속성 x ?
 * 1-2) 재고 감소 상태 관리: 재고 감소 성공 시 트랜잭션 내에서 상태 성공으로 변경.
 *
 *
 *
 * ===========================================================================
 *
 * 방법 1. 트랜잭션 범위: 상품들
 *
 * // 트랜잭션 시작
 * loop 상품: 상품들
 * 	// 전체 재고 감소. update 날리고
 * 	// 개인 재고 감소. insert 날림(개인 재고는 history로 관리 중임)
 * end
 * // 트랜잭션 종료
 *
 * -> 문제점: 데드락(상품 id 정렬, 데드락 감지 스레드, lock timoue, 트랜잭션 범위 줄인다), 지연(commit 할 때 lock 해제함. 트랜잭션 범위 줄인다)
 *
 * 방법 2. 트랜잭션 범위: 각 상품
 *
 * loop 상품: 상품들
 * // 트랜잭션 시작
 * 	// 전체 재고 감소. update 날리고
 * 	// 개인 재고 감소. insert 날림(개인 재고는 history로 관리 중임)
 * // 트랜잭션 종료
 * end
 *
 * -> 문제점: rollback 해줘야 하는데 rollback 대상들 서버 메모리 -> 서버 다운 시 날라감 -> 재고 상태 도입.
 *
 * 방법 3. 트랜잭션 범위: 각 상품 + 상태
 *
 * loop 상품: 상품들
 * 재고 상태 대기
 * // 트랜잭션 시작
 * 	// 전체 재고 감소. update 날리고
 * 	// 개인 재고 감소. insert 날림(개인 재고는 history로 관리 중임)
 * 	// 재고 상태 완료로 표시
 * // 트랜잭션 종료
 * end
 *
 * // rollback
 * loop 상품: 성공 상품들
 * // 트랜잭션 시작
 * 	// 전체 재고 증가. update 날리고
 * 	// 개인 재고 증가. insert 날림(개인 재고는 history로 관리 중임)
 * 	// 재고 상태 rollback_완료로 표시
 * // 트랜잭션 종료
 * end
 * // 주문 상태 재고 감소 완료로 변경
 *
 * -> 서버 복구 시 주문 상태 재고 대기이고 해당 주문 상품 중 재고 상태 성공인거 복구 시킴
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class ItemUpdater {
    private final SalesItemRepository salesItemRepository;
    private final StockHistoryRepository stockHistoryRepository;
    private final MemberRepository memberRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public void decreaseStockForOrder(Long buyerId, Order order, List<OrderItem> orderItems) {
        Member buyer = memberRepository.findById(buyerId).get();

        orderItems.forEach(orderItem -> doDecreaseStockForOrderPerItem(orderItem, buyer));

        // order 상태 업데이트
        orderRepository.updateStatus(order.getId(), OrderStatus.STOCK_PROCESSED);
    }

    @Transactional
    public void decreaseStockForOrderPerItem(OrderItem orderItem, Member buyer){
        doDecreaseStockForOrderPerItem(orderItem, buyer);
    }

    private void doDecreaseStockForOrderPerItem(OrderItem orderItem, Member buyer){
        SalesItem salesItem = orderItem.getSalesItem();
        Long salesItemId = salesItem.getId();
        Long requestStock = orderItem.getQuantity();

        // 전체 재고
        int affectedRows = salesItemRepository.decreaseStock(salesItemId, requestStock);
        if (affectedRows == 0) {
            log.warn("[decreaseStockForOrderPerItem][재고 부족]");
            throw new BadRequestException("재고 부족");
        }

        // 인당 재고

        Long currentPurchaseCount = stockHistoryRepository.getCurrentPurchaseCount(buyer.getId(), salesItemId);

        Long perLimitQuantity = salesItem.getPerLimitQuantity();

        if (currentPurchaseCount + requestStock > perLimitQuantity) {
            log.warn("[decreaseStockForOrderPerItem][인당 구매 개수 초과]");
            throw new BadRequestException("인당 구매 개수 초과");
        }

        // history insert

        StockHistory stockHistory = StockHistory.builder()
                .changeQuantity(requestStock)
                .stockHistoryType(StockHistoryType.PLUS)
                .message("상품 구입")
                .buyer(buyer)
                .salesItem(salesItem)
                .build();

        stockHistoryRepository.save(stockHistory);

        // 재고 상태
        orderItemRepository.updateStatus(orderItem.getId(), OrderItemStatus.SUCCESS);
    }


    @Transactional
    public void rollbackStockPerItem(SalesItem salesItem, Long quantity, Member buyer, Long orderItemId) {

        Long salesItemId = salesItem.getId();

        // 1) 전체 재고 증가 -> 이거 굳이 지금 해야 할까? 유저에게 빠르게 에러 응답하는 게 더 빠르지 않을까 생각해볼 것.
        salesItemRepository.incrementStock(salesItemId, quantity);

        // 2) 개인 재고 감소 기록

        StockHistory stockHistory = StockHistory.builder()
                .changeQuantity(quantity)
                .stockHistoryType(StockHistoryType.MINUS) // 개인 재고 감소 (구매 취소)
                .message("다른 상품 실패로 인해 개인 재고 복구")
                .buyer(buyer)
                .salesItem(salesItem)
                .build();

        stockHistoryRepository.save(stockHistory);

        // 상태
        orderItemRepository.updateStatus(orderItemId, OrderItemStatus.ROLLBACK_DONE);
    }
}
