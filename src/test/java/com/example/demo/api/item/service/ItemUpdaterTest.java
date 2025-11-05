package com.example.demo.api.item.service;

import com.example.demo.api.item.entity.Item;
import com.example.demo.api.item.entity.SalesItem;
import com.example.demo.api.item.enums.SalesItemStatus;
import com.example.demo.api.item.enums.SalesItemType;
import com.example.demo.api.item.repository.ItemRepository;
import com.example.demo.api.item.repository.SalesItemRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class ItemUpdaterTest {

    @Autowired
    private ItemUpdater itemUpdater;

    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    SalesItemRepository salesItemRepository;

    @Autowired
    EntityManager entityManager;
    @Autowired
    StringRedisTemplate stringRedisTemplate;




    @Test
    @Transactional
    void 인기_상품_올리기() throws Exception {
        // given
        Item item = new Item("item1");
        Long itemId = itemRepository.save(item).getId();

        SalesItem normalItem = SalesItem.builder()
                .remainingQuantity(7L)
                .initialQuantity(7L)
                .salesItemType(SalesItemType.NORMAL)
                .salesItemStatus(SalesItemStatus.ON)
                .item(item)
                .build();

        Long normalItemId = salesItemRepository.save(normalItem).getId();

        entityManager.flush();
        entityManager.clear();

        /*
         * 별도 스레드로 중간에 일반 상품 재고 감소 처리
         */
        Thread thread = new Thread(() -> {
            try {
                log.info("== 별도 스레드 시작 ==");
                int affectedRows = salesItemRepository.decreaseStock(normalItemId, 5L);
                log.info("[별도 스레드] affected rows: {}", affectedRows);
            } catch (Exception e) {
                log.error("별도 스레드 예외", e);
            }
        });

        thread.start();
//        thread.join(); // 스레드 끝날 때까지 대기

        log.info("== 메인 스레드 계속 진행 ==");

        Long popularItemId = itemUpdater.promoteToHotItem(normalItemId);

        entityManager.flush();
        entityManager.clear();

        // then
        SalesItem salesItem = salesItemRepository.findById(normalItemId).get();
        log.info("normalItemType= {}", salesItem.getSalesItemType());
        SalesItem salesItem1 = salesItemRepository.findById(popularItemId).get();
        log.info("popularItem= {}, {}, {}, {}, {}",
                salesItem1.getId(),
                salesItem1.getInitialQuantity(),
                salesItem1.getRemainingQuantity(),
                salesItem1.getSalesItemType(),
                salesItem1.getSalesItemStatus());

        String redisKey = "SalesItem:" + popularItemId;
        String redisValue = stringRedisTemplate.opsForValue().get(redisKey);
        log.info("redisValue= {}", redisValue);

        stringRedisTemplate.delete(redisKey);
    }

}