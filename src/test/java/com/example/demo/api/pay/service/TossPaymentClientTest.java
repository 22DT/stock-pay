package com.example.demo.api.pay.service;

import com.example.demo.api.item.ItemTestService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class TossPaymentClientTest {




    @Autowired
    TossPaymentClient tossPaymentClient;


    @Autowired
    ItemTestService itemTestService;



    @BeforeEach
    void 아이템_준비(){
        itemTestService.createItem("1", 10000000L, 10000000L);
        itemTestService.createItem("2", 10000000L, 10000000L);
        itemTestService.createItem("3", 10000000L, 10000000L);
        itemTestService.createItem("4", 10000000L, 10000000L);
        itemTestService.createItem("5", 10000000L, 10000000L);
        itemTestService.createItem("6", 10000000L, 10000000L);
        itemTestService.createItem("7", 10000000L, 10000000L);
        itemTestService.createItem("8", 10000000L, 10000000L);
        itemTestService.createItem("9", 10000000L, 10000000L);
        itemTestService.createItem("10", 10000000L, 10000000L);
    }
    
    
    @Test()
    void 결제_승인_비동기(){
       // order 생성


        // 호출
        String paymentKey="tviva20251023171841UD5H9";
        String orderId="order_1761207518848_9o2z5rhw";
        Long amount=50000L;

        log.info("[confirmPaymentAsync][before]");
        tossPaymentClient.confirmPaymentAsync(paymentKey, orderId, amount);
        log.info("[confirmPaymentAsync][after]");

    }
}