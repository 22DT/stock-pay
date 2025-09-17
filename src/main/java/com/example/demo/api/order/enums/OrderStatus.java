package com.example.demo.api.order.enums;

public enum OrderStatus {
    PENDING,            // 결제 직후, 재고 처리 대기
    STOCK_PROCESSED,    // 모든 아이템 재고 처리 완료
    FAILED    ,          // 재고 부족 등으로 주문 실패
    PAYMENT_FAILED_ROLLED_BACK

}
