package com.example.demo.api.item;

import com.example.demo.api.item.entity.Item;
import com.example.demo.api.item.entity.SalesItem;
import com.example.demo.api.item.enums.SalesItemStatus;
import com.example.demo.api.item.repository.ItemRepository;
import com.example.demo.api.item.repository.SalesItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ItemTestService {
    private final ItemRepository itemRepository;
    private final SalesItemRepository salesItemRepository;


    // 아이템 하나 생성
    @Transactional
    public void createItem(String itemName, Long totalQuantity, Long perLimitQuantity){
        // item
        Item item = new Item(itemName);

        item=itemRepository.save(item);


        // saleItem
        SalesItem salesItem = SalesItem.builder()
                .totalQuantity(totalQuantity)
                .perLimitQuantity(perLimitQuantity)
                .salesItemStatus(SalesItemStatus.ON)
                .item(item)
                .build();

        salesItemRepository.save(salesItem);
    }

    @Transactional
    public void runTransactionRandomUpdate(int i) throws InterruptedException {
        // 랜덤 순서로 salesItemId 리스트 준비
        List<Long> salesItemIds = new ArrayList<>();
        // 리스트 요소 순서 랜덤 섞기
//        Collections.shuffle(salesItemIds);

        if(i%2==0){
            salesItemIds.add(1L);
            salesItemIds.add(2L);
        }
        else{
            salesItemIds.add(2L);
            salesItemIds.add(1L);
        }


        for (Long salesItemId : salesItemIds) {
            int updated = salesItemRepository.decreaseStock(salesItemId, 1L);

            if (updated == 0) {
                throw new RuntimeException("재고 부족");
            }

            // 잠깐 sleep 넣어 데드락 가능성 높임

            Thread.sleep(500);
        }
    }
}
