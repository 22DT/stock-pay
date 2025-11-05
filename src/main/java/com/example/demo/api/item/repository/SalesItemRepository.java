package com.example.demo.api.item.repository;

import com.example.demo.api.item.entity.SalesItem;
import com.example.demo.api.item.enums.SalesItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface SalesItemRepository extends JpaRepository<SalesItem, Long> {

    @Query("select si from SalesItem si" +
            " join fetch si.item" +
            " where si.id =:salesItemId")
    Optional<SalesItem> findSalesItemsByIdWithItem(@Param("salesItemId") Long salesItemId);

    @Query("select si from SalesItem si" +
            " join fetch si.item" +
            " where si.item.id in :itemIds and si.salesItemStatus='ON'")
    List<SalesItem> findOnSalesItemsByItemIdsWithItem(@Param("itemIds") List<Long> itemIds);



    @Modifying @Transactional
    @Query("update SalesItem si set si.remainingQuantity=si.remainingQuantity-:quantity" +
            " where si.id=:salesItemId and si.remainingQuantity>=:quantity and si.salesItemStatus='ON'")
    int decreaseStock(@Param("salesItemId")Long salesItemId, @Param("quantity")Long quantity);

    @Modifying @Transactional
    @Query("update SalesItem si set si.remainingQuantity=si.remainingQuantity+:quantity where si.id=:salesItemId and si.remainingQuantity>=:quantity")
    void incrementStock(@Param("salesItemId")Long salesItemId, @Param("quantity")Long quantity);


    @Modifying
    @Transactional
    @Query("update SalesItem si set si.salesItemStatus=:salesItemStatus where si.id=:salesItemId")
    void updateSalesItemStatus(@Param("salesItemId") Long salesItemId, @Param("salesItemStatus") SalesItemStatus salesItemStatus);
}
