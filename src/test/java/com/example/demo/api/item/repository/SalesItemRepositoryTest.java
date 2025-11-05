package com.example.demo.api.item.repository;

import com.example.demo.api.item.ItemTestService;
import com.example.demo.api.item.entity.Item;
import com.example.demo.api.item.entity.SalesItem;
import com.example.demo.api.item.enums.SalesItemStatus;
import com.example.demo.api.item.enums.SalesItemType;
import com.example.demo.api.item.service.RedisStockService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
@Slf4j
class SalesItemRepositoryTest {

    @Autowired
    SalesItemRepository salesItemRepository;

    @Autowired
    ItemRepository itemRepository;

    @Autowired
    ItemTestService itemTestService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    RedisStockService redisStockService;

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



    @Test
    void 인기_상품_부하_DB() throws InterruptedException {
        // given
        Item item = new Item("item1");
        itemRepository.save(item);

        SalesItem popularItem = SalesItem.builder()
                .remainingQuantity(1_000_000L)
                .initialQuantity(1_000_000L)
                .salesItemType(SalesItemType.NORMAL)
                .salesItemStatus(SalesItemStatus.ON)
                .item(item)
                .build();

        Long popularItemId = salesItemRepository.save(popularItem).getId();

        // when
        int threadCount = 10000; // 동시성 시뮬레이션할 스레드 개수
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    salesItemRepository.decreaseStock(popularItemId, 1L);
                } catch (Exception e) {
                    log.error("error: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long endTime = System.currentTimeMillis();

        // then
        long duration = endTime - startTime;
        log.info("DB 부하 테스트 완료 — 총 소요 시간: {} ms", duration);

        SalesItem result = salesItemRepository.findById(popularItemId).orElseThrow();
        log.info("최종 재고: {}", result.getRemainingQuantity());
    }


    @Test
    void 인기_상품_부하_Redis() throws InterruptedException {
        // given
        String redisKey = "SalesItem:" + 1;
        stringRedisTemplate.opsForValue().set(redisKey, "1000000");

        int threadCount = 10000; // 동시 요청 개수
        ExecutorService executor = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(threadCount);

        long startTime = System.currentTimeMillis();

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    redisStockService.decreaseStock(redisKey, 1);
                } catch (Exception e) {
                    log.error("Redis error: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long endTime = System.currentTimeMillis();

        // then
        long duration = endTime - startTime;
        String remaining = stringRedisTemplate.opsForValue().get(redisKey);

        log.info("Redis 부하 테스트 완료 — 총 소요 시간: {} ms", duration);
        log.info("최종 Redis 재고: {}", remaining);

        // 테스트 종료 후 Redis 정리
        stringRedisTemplate.delete(redisKey);
    }




}