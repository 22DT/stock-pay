package com.example.demo.api.item.repository;

import com.example.demo.api.item.ItemTestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest

class SalesItemRepositoryTest {

    @Autowired
    SalesItemRepository salesItemRepository;


    @Autowired
    ItemTestService itemTestService;

    @Test
    void 데드락_실험() throws InterruptedException {
        int threadCount = 100; // 동시에 실행할 스레드 수
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            int finalI = i;
            executor.submit(() -> {
                try {
                    itemTestService.runTransactionRandomUpdate(finalI);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
    }





}