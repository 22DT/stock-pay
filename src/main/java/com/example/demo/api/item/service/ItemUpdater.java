package com.example.demo.api.item.service;

import com.example.demo.api.item.dto.internal.StockDecreaseContext;
import com.example.demo.api.item.entity.SalesItem;
import com.example.demo.api.item.entity.StockHistory;
import com.example.demo.api.item.enums.StockHistoryType;
import com.example.demo.api.item.repository.SalesItemRepository;
import com.example.demo.api.item.repository.StockHistoryRepository;
import com.example.demo.api.order.entity.Order;
import com.example.demo.api.order.entity.OrderItem;
import com.example.demo.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ItemUpdater {
    private final SalesItemRepository salesItemRepository;
    private final StockHistoryRepository stockHistoryRepository;


    @Transactional
    public void decreaseStockForOrder(StockDecreaseContext context) {
        Order order = context.order();
        List<OrderItem> orderItems = context.orderItems();
        Map<Long, SalesItem> itemIdToSalesItem = context.itemIdToSalesItem();
        Map<Long, Long> itemIdToSalesItemId = context.itemIdToSalesItemId();

        orderItems.forEach(orderItem -> {

            Long itemId = orderItem.getItem().getId();
            Long requestStock = orderItem.getQuantity();
    
            // 전체 재고
            Long salesItemId = itemIdToSalesItemId.get(itemId); //
            Long affectedRows = salesItemRepository.decreaseStock(salesItemId, requestStock);

            if (affectedRows == 0) {
                throw new BadRequestException("재고 부족");
            }

            // 인당 재고

            List<StockHistory> stockHistories = stockHistoryRepository.findByOrderIdAndSalesItemId(order.getId(), salesItemId);
            long cnt=0;

            for (StockHistory stockHistory : stockHistories) {
                long tmpCnt = stockHistory.getChangeQuantity();
                if(stockHistory.getStockHistoryType().equals(StockHistoryType.MINUS)){
                    tmpCnt *= -1;
                }

                cnt += tmpCnt;

            }

            SalesItem salesItem = itemIdToSalesItem.get(itemId);
            Long perLimitQuantity = salesItem.getPerLimitQuantity();

            if(cnt+requestStock>perLimitQuantity){
                throw new BadRequestException("인당 구매 개수 초과");
            }


            // history insert

            StockHistory stockHistory = StockHistory.builder()
                    .changeQuantity(requestStock)
                    .stockHistoryType(StockHistoryType.PLUS)
                    .message("상품 구입")
                    .salesItem(salesItem)
                    .order(order)
                    .build();

            stockHistoryRepository.save(stockHistory);

        });
    }


}
