package com.example.demo.api.item.repository;

import com.example.demo.api.item.entity.SalesItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SalesItemRepository extends JpaRepository<SalesItem, Long> {

    @Query("select si from SalesItem si" +
            " join fetch si.item" +
            " where si.item.id in :itemIds and si.salesItemStatus='ON'")
    List<SalesItem> findOnSalesItemsByItemIdsWithItem(@Param("itemIds") List<Long> itemIds);



    @Modifying @Transactional
    @Query("update SalesItem si set si.totalQuantity=si.totalQuantity-:quentity where si.id=:salesItemId and si.totalQuantity>=:quantity")
    Long decreaseStock(@Param("salesItemId")Long salesItemId, @Param("quantity")Long quantity);
}
