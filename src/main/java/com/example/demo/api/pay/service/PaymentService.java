package com.example.demo.api.pay.service;

import com.example.demo.api.order.repository.OrderRepository;
import com.example.demo.api.order.service.OrderProcessor;
import com.example.demo.api.pay.dto.internal.PaymentDTO;
import com.example.demo.api.pay.dto.request.PaymentConfirmRequestDTO;
import com.example.demo.api.pay.enums.PaymentStatus;
import com.example.demo.api.pay.exception.PaymentTimeoutException;
import com.example.demo.api.pay.exception.confirm.PaymentAbortedException;
import com.example.demo.api.pay.exception.confirm.PaymentAlreadyDoneException;
import com.example.demo.api.pay.exception.confirm.PaymentExpiredException;
import com.example.demo.api.pay.repository.PaymentRepository;
import com.example.demo.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.example.demo.common.response.ErrorStatus.ALREADY_DONE_PAYMENT_BEFORE_ORDER_EXCEPTION;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final TossPaymentClient tossPaymentClient;
    private final PaymentProcessor paymentProcessor;
    private final OrderProcessor orderProcessor;


    /**
     * 결제 상태는 반드시 IN_PROGRESS 로 시작하여 성공(DONE), 실패(ABORTED, RETRY, EXPIRED), 타임아웃(TIMEOUT) 중 하나로 끝나야 한다.
     *
     * 1) 구입할 수 있는지 각종 검증: amount, 최초 요청 등
     * 2) 결제 승인 api 호출
     * 3) db 반영
     *
     *
     * 1)
     * amount 검증.
     * 최초 요청 || RETRY 만 통과시킨다.
     *
     * 2) 결제 승인 api 호출
     * 결제 승인 결과: 성공(ALREADY_DONE, DONE), 실패(ABORTED, RETRY, EXPIRED), 타임아웃(TIMEOUT)
     * 실패: 결제 승인 x 확신 -> 상품 관련 만 rollback
     * 타임아웃: 결제 승인 유무 모름 -> 후보정
     *
     * 3) db 반영
     *
     * =============
     *
     * 결제 승인 성공은 반드시 이 api 를 통해서만 할 것.
     *
     *
     */

    public void requestConfirm(Long buyerId, PaymentConfirmRequestDTO paymentConfirmRequestDTO) {
        String paymentKey = paymentConfirmRequestDTO.paymentKey();
        String merchantOrderId = paymentConfirmRequestDTO.merchantOrderId();
        String amount = paymentConfirmRequestDTO.amount();

        // 1)

        amountValidate(amount, merchantOrderId);

        paymentRepository.findByPaymentKey(paymentKey)
                .ifPresent(p -> {
                    throw new BadRequestException("이미 존재하는 결제 정보입니다.");
                });


        /*
         * 여기에  update 가 필요한 검증.
         */

        orderProcessor.processStockOnOrderV3(buyerId, merchantOrderId);


        try{
            // 2
            PaymentDTO paymentDTO = tossPaymentClient.confirmPayment(paymentKey, merchantOrderId, Long.valueOf(amount));

            // 3
            paymentProcessor.completePayment(paymentDTO);

        }catch(PaymentAlreadyDoneException e){
            PaymentDTO paymentDTO = tossPaymentClient.getPaymentByPaymentKey(paymentKey);
            paymentProcessor.completePayment(paymentDTO);

        }catch (PaymentAbortedException e){
            orderProcessor.stockRollback(buyerId, merchantOrderId);
            paymentRepository.updatePaymentStatusByPaymentKey(paymentKey, PaymentStatus.ABORTED);
            throw e;

        }catch(PaymentExpiredException e){
            orderProcessor.stockRollback(buyerId, merchantOrderId);
            paymentRepository.updatePaymentStatusByPaymentKey(paymentKey, PaymentStatus.EXPIRED);
            throw e;

        }catch(PaymentTimeoutException e){
            paymentRepository.updatePaymentStatusByPaymentKey(paymentKey, PaymentStatus.TIMEOUT);
            throw e;
        }catch (Exception e){
            log.error("[requestConfirm][알 수 없는 오류]", e);
            throw e;
        }
    }

    private void amountValidate(String amount, String merchantOrderId){
        String findAmount = orderRepository.findAmountByMerchantOrderId(merchantOrderId);

        if(findAmount==null || findAmount.isEmpty()){
            log.warn("[requestConfirm][주문도 안 했는데 벌써 결제를 해?][merchantOrderId= {}]", merchantOrderId);
            throw new BadRequestException(ALREADY_DONE_PAYMENT_BEFORE_ORDER_EXCEPTION.getMessage());
        }

        if(!findAmount.equals(amount)) {
            log.warn("[requestConfirm][amount error][findAmount= {}, requestAmount= {}]", findAmount, amount);
            throw new BadRequestException("결제 처음부터 다시 시도.");
        }
    }
}
