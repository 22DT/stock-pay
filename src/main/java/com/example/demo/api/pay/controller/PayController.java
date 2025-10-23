package com.example.demo.api.pay.controller;

import com.example.demo.api.pay.dto.request.PaymentConfirmRequestDTO;
import com.example.demo.api.pay.service.TossPaymentClient;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "test", description = "결제 관련 API 입니다.")
@RestController
@RequestMapping("/api/v2/pay")
@RequiredArgsConstructor
@Slf4j
public class PayController {
    private final TossPaymentClient tossPaymentClient;



    @PostMapping("/confirm")
    public ResponseEntity<String> controller(@RequestBody PaymentConfirmRequestDTO dto) {
        tossPaymentClient.confirmPaymentAsync(dto.paymentKey(), dto.merchantOrderId(), Long.valueOf( dto.amount()));
        log.info("[confirmPayment][accepted] merchantOrderId={}. paymentKey= {}", dto.merchantOrderId(), dto.paymentKey());
        return ResponseEntity.accepted().body("결제 승인 요청이 접수되었습니다.");
    }
}
