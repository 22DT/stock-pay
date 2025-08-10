package com.example.demo.api.pay.service;

import com.example.demo.api.pay.dto.internal.PaymentCancelDTO;
import com.example.demo.api.pay.dto.internal.PaymentDTO;
import com.example.demo.api.pay.entity.Payment;
import com.example.demo.api.pay.entity.PaymentReconcileHistory;
import com.example.demo.api.pay.enums.PaymentStatus;
import com.example.demo.api.pay.enums.TossPaymentStatus;
import com.example.demo.api.pay.exception.PaymentException;
import com.example.demo.api.pay.exception.cancel.PaymentAlreadyCancelException;
import com.example.demo.api.pay.repository.PaymentReconcileHistoryRepository;
import com.example.demo.api.pay.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentScheduler {
    private final PaymentRepository paymentRepository;
    private final TossPaymentClient tossPaymentClient;
    private final PaymentProcessor paymentProcessor;
    private final PaymentReconcileHistoryRepository paymentReconcileHistoryRepository;


//    @Scheduled(fixedDelay = 120000) // ms 새벽 3시쯤에 돌게 해야 함.
    public void resolveTimeoutPayments() {
        log.info("[resolveTimeoutPayments][call]");
        // 한 번에 몇 개? 그리고 몇 번 나중에 조절
        Pageable pageable = PageRequest.of(0, 10);
        List<Payment> batch= paymentRepository.findPaymentsCreatedBefore(
                List.of(PaymentStatus.TIMEOUT, PaymentStatus.IN_PROGRESS),
                LocalDate.now().minusDays(1),
                pageable
        );

        log.info("[batch.size()= {}]", batch.size());

        batch.forEach(payment -> {
            // 조회
            String paymentKey = payment.getPaymentKey();
            PaymentDTO paymentDTO;

            try {
                paymentDTO = tossPaymentClient.getPaymentByPaymentKey(paymentKey);
            } catch (PaymentException e) {
                log.warn("[resolveTimeoutPayments][조회 실패][PaymentException][paymentKey= {}]", paymentKey, e);

                // reconcile 테이블에 기록
                PaymentReconcileHistory paymentReconcileHistory = PaymentReconcileHistory.failure(paymentKey, payment.getPaymentStatus(), "TIMEOUT 및 IN_PROGRESS 후보정", e.getMessage());
                paymentReconcileHistoryRepository.save(paymentReconcileHistory);

                return; // 이 payment 는 이번 배치에서는 skip
            } catch (Exception e) {
                // 예상치 못한 예외 (ex: JSON 파싱 실패)
                log.error("[resolveTimeoutPayments][조회 실패][Unknown Exception][paymentKey= {}]", paymentKey, e);

                // reconcile 테이블에 기록
                PaymentReconcileHistory paymentReconcileHistory = PaymentReconcileHistory.failure(paymentKey, payment.getPaymentStatus(), "TIMEOUT 및 IN_PROGRESS 후보정", e.getMessage());
                paymentReconcileHistoryRepository.save(paymentReconcileHistory);

                return; // skip
            }


            TossPaymentStatus tossPaymentStatus = paymentDTO.tossPaymentStatus();
            log.info("[resolveTimeoutPayments][조회 성공][paymentKey= {}, tossPaymentStatus= {}]", paymentKey, tossPaymentStatus);

            switch (tossPaymentStatus) {
                case ABORTED, EXPIRED:
                    PaymentStatus paymentStatus = PaymentStatus.fromTossStatus(tossPaymentStatus);

                    paymentProcessor.reconcileAbortedAndExpired(paymentKey, payment.getPaymentStatus(), paymentStatus);

                    break;

                case DONE:
                    // toss cancel api
                    List<PaymentCancelDTO> paymentCancelDTOS;
                    try {
                        paymentCancelDTOS = tossPaymentClient.cancelPayment(paymentKey, "timeout 에러 후처리");

                        // db 반영: 1) 결제 승인  2) 결제 취소.
                        paymentProcessor.reconcileDoneAndCanceled(paymentDTO, paymentCancelDTOS, payment.getPaymentStatus());

                    }catch (PaymentAlreadyCancelException e) {

                        // db 반영: 1) 결제 승인  2) 결제 취소.
                        paymentProcessor.reconcileDoneAndCanceled(paymentDTO, Collections.emptyList(), payment.getPaymentStatus());

                    }catch (PaymentException e){
                        log.warn("[resolveTimeoutPayments][취소 실패][PaymentException][paymentKey= {}]", paymentKey, e);

                        // reconcile 테이블에 기록
                        PaymentReconcileHistory paymentReconcileHistory = PaymentReconcileHistory.failure(paymentKey, payment.getPaymentStatus(), "TIMEOUT 및 IN_PROGRESS 후보정", e.getMessage());
                        paymentReconcileHistoryRepository.save(paymentReconcileHistory);

                        return;

                    }catch(Exception e){
                        // 예상치 못한 예외 (ex: JSON 파싱 실패)
                        log.error("[resolveTimeoutPayments][취소 실패][Unknown Exception][paymentKey= {}]", paymentKey, e);

                        // reconcile 테이블에 기록
                        PaymentReconcileHistory paymentReconcileHistory = PaymentReconcileHistory.failure(paymentKey, payment.getPaymentStatus(), "TIMEOUT 및 IN_PROGRESS 후보정", e.getMessage());
                        paymentReconcileHistoryRepository.save(paymentReconcileHistory);

                        return; // skip
                    }

                    break;

                case IN_PROGRESS:
                    // 승인 요청 안 한 거임. 
                    log.info("[resolveTimeoutPayments][IN_PROGRESS 상태 발생][paymentKey= {}]", paymentKey);
                    break;
            }
        });
    }
}
