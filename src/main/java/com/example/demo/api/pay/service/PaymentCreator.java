package com.example.demo.api.pay.service;

import com.example.demo.api.order.entity.Order;
import com.example.demo.api.order.repository.OrderRepository;
import com.example.demo.api.pay.entity.Payment;
import com.example.demo.api.pay.enums.PaymentStatus;
import com.example.demo.api.pay.repository.PaymentRepository;
import com.example.demo.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.example.demo.common.response.ErrorStatus.ALREADY_DONE_PAYMENT_BEFORE_ORDER_EXCEPTION;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCreator {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public Payment create(String paymentKey, String orderId) {
        Order order = orderRepository.findByMerchantOrderId(orderId)
                .orElseThrow(() -> {
                    log.warn("[requestConfirm][주문도 안 했는데 벌써 결제를 해?][merchantOrderId= {}]", orderId);
                    return new BadRequestException(ALREADY_DONE_PAYMENT_BEFORE_ORDER_EXCEPTION.getMessage());
                });

        Payment payment = Payment.builder()
                .paymentKey(paymentKey)
                .paymentStatus(PaymentStatus.IN_PROGRESS)
                .order(order)
                .build();

        Payment save = paymentRepository.save(payment);

        return save;
    }
}
