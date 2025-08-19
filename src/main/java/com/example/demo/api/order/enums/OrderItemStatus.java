package com.example.demo.api.order.enums;

public enum OrderItemStatus {
    PENDING,            // 초기 상태 (재고 처리 전)
    SUCCESS,            // 재고 감소 성공
    CANCELLED,          // 주문 취소됨
    ROLLBACK_DONE       // rollback 완료
}
