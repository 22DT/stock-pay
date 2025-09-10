package com.example.demo.api.item.repository;

import com.example.demo.api.item.entity.MemberItemStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface MemberItemStockRepository extends JpaRepository<MemberItemStock, Long> {

    @Query("select count(*)>0 from MemberItemStock mis" +
            " where mis.buyer.id=:buyerId and mis.salesItem.id =:salesItemId")
    boolean existsByBuyerIdAndSalesItemId(@Param("buyerId") Long buyerId, @Param("salesItemId")Long salesItemId);



    @Modifying @Transactional
    @Query("update MemberItemStock mis set mis.remainingQuantity=mis.remainingQuantity-:quen")
}
