package com.example.demo.api.pay.service;

import com.example.demo.api.pay.dto.internal.PaymentCancelDTO;
import com.example.demo.api.pay.dto.internal.PaymentDTO;
import com.example.demo.api.pay.entity.PaymentCancelHistory;
import com.example.demo.api.pay.entity.PaymentHistory;
import com.example.demo.api.pay.entity.PaymentReconcileHistory;
import com.example.demo.api.pay.enums.PaymentStatus;
import com.example.demo.api.pay.repository.PaymentCancelHistoryRepository;
import com.example.demo.api.pay.repository.PaymentHistoryRepository;
import com.example.demo.api.pay.repository.PaymentReconcileHistoryRepository;
import com.example.demo.api.pay.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;


/**
 *  공통 or 부분적으로 트랜잭션이 필요한 로직  여기에서 작업함.
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentProcessor {
    private final PaymentUpdater paymentUpdater;
    private final PaymentRepository paymentRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final PaymentCancelHistoryRepository paymentCancelHistoryRepository;
    private final PaymentReconcileHistoryRepository paymentReconcileHistoryRepository;


    @Transactional
    public void completePayment(PaymentDTO paymentDTO){
        String paymentKey = paymentDTO.paymentKey();
        String amount = paymentDTO.totalAmount();
        OffsetDateTime approvedAt = paymentDTO.approvedAt();

        paymentUpdater.updatePaymentAsDone(paymentDTO);

        PaymentHistory build = PaymentHistory.builder()
                .paymentKey(paymentKey)
                .amount(amount)
                .approvedAt(approvedAt)
                .build();

        paymentHistoryRepository.save(build);

    }

    @Transactional
    public void cancelPayment(String paymentKey, List<PaymentCancelDTO> paymentCancelDTOList, PaymentStatus paymentStatus){
        // 상태 update
        paymentRepository.updatePaymentStatusByPaymentKey(paymentKey, paymentStatus);

        // history
        for (PaymentCancelDTO paymentCancelDTO : paymentCancelDTOList) {
            String transactionKey = paymentCancelDTO.transactionKey();
            String cancelAmount = paymentCancelDTO.cancelAmount();
            String cancelReason = paymentCancelDTO.cancelReason();
            OffsetDateTime canceledAt = paymentCancelDTO.canceledAt();

            PaymentCancelHistory build = PaymentCancelHistory.builder()
                    .paymentKey(paymentKey)
                    .transactionKey(transactionKey)
                    .cancelAmount(cancelAmount)
                    .cancelReason(cancelReason)
                    .canceledAt(canceledAt)
                    .build();

            paymentCancelHistoryRepository.save(build);

        }
    }

    @Transactional
    public void reconcileDoneAndCanceled(PaymentDTO paymentDTO, List<PaymentCancelDTO> paymentCancelDTOList, PaymentStatus prevStatus){
        String paymentKey = paymentDTO.paymentKey();
        completePayment(paymentDTO);
        cancelPayment(paymentKey, paymentCancelDTOList, PaymentStatus.DONE_CANCELED);

        PaymentReconcileHistory paymentReconcileHistory = PaymentReconcileHistory.success(paymentKey, prevStatus, PaymentStatus.DONE_CANCELED, "TIMEOUT 및 IN_PROGRESS 후보정", null);
        paymentReconcileHistoryRepository.save(paymentReconcileHistory);

    }

    @Transactional
    public void reconcileAbortedAndExpired(String paymentKey, PaymentStatus prevStatus, PaymentStatus newStatus){
        paymentRepository.updatePaymentStatusByPaymentKey(paymentKey, newStatus);

        PaymentReconcileHistory paymentReconcileHistory = PaymentReconcileHistory.success(paymentKey, prevStatus, newStatus, "TIMEOUT 및 IN_PROGRESS 후보정", null);
        paymentReconcileHistoryRepository.save(paymentReconcileHistory);
    }
}
