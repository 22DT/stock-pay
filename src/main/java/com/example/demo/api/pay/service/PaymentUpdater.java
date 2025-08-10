package com.example.demo.api.pay.service;

import com.example.demo.api.pay.dto.internal.PaymentDTO;
import com.example.demo.api.pay.entity.Payment;
import com.example.demo.api.pay.repository.PaymentRepository;
import com.example.demo.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentUpdater {
    private final PaymentRepository paymentRepository;


    @Transactional
    public void updatePaymentAsDone(PaymentDTO paymentDTO){
        String paymentKey = paymentDTO.paymentKey();

        Payment payment = paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> {
                    log.warn("[updatePaymentAsDone][payment 없음.][paymentKey={}]", paymentKey);
                    return new NotFoundException("payment 없음.");
                });

        payment.updatePayment(paymentDTO);
    }
}
