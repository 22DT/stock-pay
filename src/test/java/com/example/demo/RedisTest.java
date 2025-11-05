package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class RedisTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @Test
    void testRedis() {
        // given
        stringRedisTemplate.opsForValue().set("hello", "world");

        // when
        String value = stringRedisTemplate.opsForValue().get("hello");

        // then
        System.out.println("Redis value: " + value);
        assertThat(value).isEqualTo("world");


    }
}
