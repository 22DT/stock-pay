package com.example.demo.api.item;

import com.example.demo.api.item.service.RedisStockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ItemTestServiceTest {

    @Autowired
    ItemTestService itemTestService;





    @Test
    @Rollback(false)
    void 데이터_준비(){
        itemTestService.createItem("1", 100L, 10L);
        itemTestService.createItem("2", 100L, 10L);
        itemTestService.createItem("3", 100L, 10L);
    }


    @Test
    @Rollback(false)
    void 데이터_준비V2(){
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
}