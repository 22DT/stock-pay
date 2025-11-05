package com.example.demo.api.item.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisStockService {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisStockService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public synchronized void decreaseStock(String redisKey, int quantity) {
        String stockStr = stringRedisTemplate.opsForValue().get(redisKey);
        if (stockStr == null) {
            throw new IllegalStateException("재고 정보가 없습니다.");
        }

        int currentStock;
        try {
            currentStock = Integer.parseInt(stockStr);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("재고 데이터 형식이 잘못되었습니다.");
        }

        if (currentStock < quantity) {
            throw new IllegalStateException("재고가 부족합니다. (현재 재고: " + currentStock + ")");
        }

        int newStock = currentStock - quantity;
        stringRedisTemplate.opsForValue().set(redisKey, String.valueOf(newStock));
    }
}

