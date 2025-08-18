package com.example.demo.api.item;

import com.example.demo.api.item.entity.Item;
import com.example.demo.api.item.entity.SalesItem;
import com.example.demo.api.item.enums.SalesItemStatus;
import com.example.demo.api.item.repository.ItemRepository;
import com.example.demo.api.item.repository.SalesItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
}
