package com.example.demo.api.item;

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
}