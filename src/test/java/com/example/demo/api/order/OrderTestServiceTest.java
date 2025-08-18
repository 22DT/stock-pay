package com.example.demo.api.order;

import com.example.demo.api.item.service.ItemUpdater;
import com.example.demo.api.order.dto.request.OrderRequestDTO;
import com.example.demo.api.order.service.OrderProcessor;
import com.example.demo.api.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OrderTestServiceTest {
    @Autowired
    OrderService orderService;

    @Autowired
    OrderProcessor orderProcessor;


    
    @Rollback(false)
    @Test
    void 구입_성공() {
        /*
        * 주문만들기 (주문 api 호출)
        * */

        Long buyerId = 1L;

        String merchantOrderId= UUID.randomUUID().toString();
        String amount = "50000";
        List<OrderRequestDTO.ItemQuantity> itemQuantities = new ArrayList<>();

        itemQuantities.add(new OrderRequestDTO.ItemQuantity(1L, 5L));
        itemQuantities.add(new OrderRequestDTO.ItemQuantity(2L, 10L));
        itemQuantities.add(new OrderRequestDTO.ItemQuantity(3L, 20L));

        orderService.createOrder(buyerId, new OrderRequestDTO(merchantOrderId, amount, itemQuantities));


        /*
        * 재고 (결제 api 호출 시)
        * */

        orderProcessor.processStockOnOrderV2(buyerId, merchantOrderId);

    }

}