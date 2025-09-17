package com.example.demo.api.order;

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        itemQuantities.add(new OrderRequestDTO.ItemQuantity(3L, 5L));

        orderService.createOrder(buyerId, new OrderRequestDTO(merchantOrderId, amount, itemQuantities));


        /*
        * 재고 (결제 api 호출 시)
        * */

        orderProcessor.processStockOnOrderV2(buyerId, merchantOrderId);

    }



    @Test
    void 방법1_데드락_발생() throws InterruptedException {
        Long buyerId = 1L;


        // 재고 처리 스레드 실행
        int threadCount = 100; // 동시에 실행할 스레드 수
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            int finalI = i;
            executor.submit(() -> {
                try {

                    // 주문 생성
                    String merchantOrderId= UUID.randomUUID().toString();
                    String amount = "50000";
                    List<OrderRequestDTO.ItemQuantity> itemQuantities = new ArrayList<>();

                    itemQuantities.add(new OrderRequestDTO.ItemQuantity(1L, 1L));
                    itemQuantities.add(new OrderRequestDTO.ItemQuantity(2L, 1L));
                    itemQuantities.add(new OrderRequestDTO.ItemQuantity(3L, 1L));

                    orderService.createOrder(buyerId, new OrderRequestDTO(merchantOrderId, amount, itemQuantities));

                    // 생성된 주문에 대해 재고 감소.

                    orderProcessor.processStockOnOrder(buyerId, merchantOrderId);

                } catch (Exception e) {
                    System.out.println("예외");
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드 종료 대기
        executor.shutdown();

    }

    @Test
    void 방법2_데드락_해결() throws InterruptedException {
        Long buyerId = 1L;


        // 재고 처리 스레드 실행
        int threadCount = 100; // 동시에 실행할 스레드 수
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            int finalI = i;
            executor.submit(() -> {
                try {

                    // 주문 생성
                    String merchantOrderId= UUID.randomUUID().toString();
                    String amount = "50000";
                    List<OrderRequestDTO.ItemQuantity> itemQuantities = new ArrayList<>();

                    itemQuantities.add(new OrderRequestDTO.ItemQuantity(1L, 1L));
                    itemQuantities.add(new OrderRequestDTO.ItemQuantity(2L, 1L));
                    itemQuantities.add(new OrderRequestDTO.ItemQuantity(3L, 1L));


                    orderService.createOrder(buyerId, new OrderRequestDTO(merchantOrderId, amount, itemQuantities));

                    // 생성된 주문에 대해 재고 감소.

                    orderProcessor.processStockOnOrderV2(buyerId, merchantOrderId);

                } catch (Exception e) {
                    System.out.println("예외");
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드 종료 대기
        executor.shutdown();

    }
}