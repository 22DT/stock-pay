package com.example.demo.api.item.repository;

import com.example.demo.api.item.entity.StockHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {

    @Query("select sh from StockHistory sh" +
            " where sh.buyer.id=:buyerId and sh.salesItem.id=:salesItemId")
    List<StockHistory> findByBuyerIdAndSalesItemId(@Param("buyerId")Long buyerId, @Param("salesItemId") Long salesItemId);

    @Query("select coalesce(sum(" +
            "case when sh.stockHistoryType='PLUS' then sh.changeQuantity" +
            " when sh.stockHistoryType='MINUS' then -sh.changeQuantity" +
            " else 0" +
            " end), 0)" +
            " from StockHistory sh" +
            " where sh.buyer.id=:buyerId and sh.salesItem.id=:salesItemId")
    Long getCurrentPurchaseCount(@Param("buyerId") Long buyerId, @Param("salesItemId") Long salesItemId);

}
