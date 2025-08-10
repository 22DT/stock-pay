package com.example.demo.api.pay.dto.external.response;

import java.time.OffsetDateTime;
import java.util.List;

public record TossPaymentResponseDTO(
        String mId,                            // 가맹점 ID (Merchant ID)
        String lastTransactionKey,            // 마지막 거래 키 (마지막 결제/취소 거래 식별자)
        String paymentKey,                    // 결제 고유 키 (결제 건 식별자)
        String orderId,                      // 주문 번호 (가맹점 주문 식별자)
        String orderName,                    // 주문명 (상품 또는 서비스 이름)
        int taxExemptionAmount,              // 면세 금액
        String status,                      // 결제 상태 (예: READY, APPROVED, FAILED 등)
        OffsetDateTime requestedAt,          // 결제 요청 시각
        OffsetDateTime approvedAt,           // 결제 승인 시각 (승인 완료 시점)
        boolean useEscrow,                  // 에스크로 사용 여부 (true: 에스크로 결제)
        boolean cultureExpense,              // 문화비 지출 여부 (true: 문화비 결제)
        Card card,                          // 카드 결제 정보 (카드 결제 시 포함)
        VirtualAccount virtualAccount,      // 가상계좌 결제 정보 (가상계좌 결제 시 포함)
        Transfer transfer,                  // 계좌이체 결제 정보 (계좌이체 시 포함)
        MobilePhone mobilePhone,            // 휴대폰 결제 정보 (휴대폰 결제 시 포함)
        GiftCertificate giftCertificate,    // 상품권 결제 정보 (상품권 결제 시 포함)
        CashReceipt cashReceipt,            // 단일 현금영수증 정보
        List<CashReceipt> cashReceipts,     // 복수 현금영수증 정보 (여러 건일 경우)
        Discount discount,                  // 할인 정보
        List<Cancel> cancels,               // 취소 내역 리스트
        String secret,                      // 결제 보안 관련 키 (비공개 정보)
        String type,                        // 결제 유형 (예: NORMAL, ESCROW 등)
        EasyPay easyPay,                    // 간편결제 정보 (카카오페이, 네이버페이 등)
        String country,                    // 결제 국가 코드
        Failure failure,                  // 실패 정보 (결제 실패 시 포함)
        boolean isPartialCancelable,        // 부분 취소 가능 여부
        Receipt receipt,                  // 영수증 정보
        Checkout checkout,                // 체크아웃 URL 정보
        String currency,                  // 통화 단위 (예: KRW, USD)
        int totalAmount,                  // 총 결제 금액
        int balanceAmount,                // 잔여 금액 (부분 취소 등 시 남은 금액)
        int suppliedAmount,               // 공급가액 (부가세 제외 금액)
        int vat,                         // 부가세 금액
        int taxFreeAmount,                // 면세 금액
        Object metadata,                  // 기타 메타데이터 (결제 관련 추가 정보)
        String method,                   // 결제 수단 (예: CARD, TRANSFER, MOBILE_PHONE 등)
        String version                   // API 응답 버전 정보
) {
    public record Card(
            String issuerCode,              // 카드 발급사 코드
            String acquirerCode,            // 카드 매입사 코드
            String number,                  // 카드 번호 (일부 마스킹 포함)
            int installmentPlanMonths,     // 할부 개월 수
            boolean isInterestFree,         // 무이자 할부 여부
            String interestPayer,           // 이자 부담자 (가맹점, 고객 등)
            String approveNo,               // 승인 번호
            boolean useCardPoint,           // 카드 포인트 사용 여부
            String cardType,                // 카드 종류 (신용, 체크 등)
            String ownerType,               // 카드 소유자 유형 (개인, 법인 등)
            String acquireStatus,           // 매입 상태
            Long amount                     // 카드 결제 금액
    ) {}

    public record VirtualAccount(
            String accountType,             // 가상계좌 종류 (예: NORMAL, ESCROW 등)
            String accountNumber,           // 가상계좌 번호
            String bankCode,                // 은행 코드
            String customerName,            // 예금주 이름
            OffsetDateTime dueDate,         // 입금 마감일
            String refundStatus,            // 환불 상태
            boolean expired,                // 입금 기간 만료 여부
            String settlementStatus,        // 정산 상태
            RefundReceiveAccount refundReceiveAccount  // 환불 수취 계좌 정보 (nullable)
    ) {}

    public record RefundReceiveAccount(
            String bankCode,               // 은행 코드
            String accountNumber,          // 계좌 번호
            String holderName              // 예금주명
    ) {}

    public record Transfer(
            String bankCode,               // 은행 코드
            String settlementStatus        // 정산 상태
    ) {}

    public record MobilePhone(
            String customerMobilePhone,    // 고객 휴대폰 번호 (숫자만, 8~15자리)
            String settlementStatus,       // 정산 상태
            String receiptUrl              // 영수증 URL
    ) {}

    public record GiftCertificate(
            String approveNo,              // 승인 번호
            String settlementStatus        // 정산 상태
    ) {}

    public record CashReceipt(
            String type,                  // 영수증 종류 (소득공제, 지출증빙)
            String receiptKey,            // 영수증 키
            String issueNumber,           // 발급 번호
            String receiptUrl,            // 영수증 URL
            int amount,                  // 금액
            int taxFreeAmount,            // 면세 금액
            String orderId,               // 주문 번호
            String orderName,             // 주문명
            String businessNumber,        // 사업자 번호
            String transactionType,       // 거래 유형
            String issueStatus,           // 발급 상태
            Failure failure,              // 실패 정보 (nullable)
            String customerIdentityNumber,// 고객 식별 번호
            OffsetDateTime requestedAt    // 요청 시각
    ) {}

    public record Discount(
            int amount                    // 할인 금액
    ) {}

    public record Cancel(
            int cancelAmount,             // 취소 금액
            String cancelReason,          // 취소 사유
            int taxFreeAmount,            // 면세 취소 금액
            int taxExemptionAmount,       // 면세 제외 취소 금액
            int refundableAmount,          // 환불 가능 금액
            int transferDiscountAmount,    // 계좌이체 할인 취소 금액
            int easyPayDiscountAmount,     // 간편결제 할인 취소 금액
            OffsetDateTime canceledAt,     // 취소 시각
            String transactionKey,         // 거래 키
            String receiptKey,             // 영수증 키 (nullable)
            String cancelStatus,           // 취소 상태
            String cancelRequestId         // 취소 요청 ID (nullable)
    ) {}

    public record Failure(
            String code,                  // 실패 코드
            String message                // 실패 메시지
    ) {}

    public record EasyPay(
            String provider,              // 간편결제 제공사 (카카오페이, 네이버페이 등)
            int amount,                  // 결제 금액
            int discountAmount           // 할인 금액
    ) {}

    public record Receipt(
            String url                   // 영수증 URL
    ) {}

    public record Checkout(
            String url                   // 체크아웃 URL (결제 페이지 등)
    ) {}
}
