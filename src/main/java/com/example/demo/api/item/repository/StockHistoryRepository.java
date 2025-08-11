package com.example.demo.api.item.repository;

import com.example.demo.api.item.entity.StockHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {

    @Query("select sh from StockHistory sh" +
            " where sh.order.id=:orderId and sh.salesItem.id=:salesItemId")
    List<StockHistory> findByOrderIdAndSalesItemId(@Param("orderId")Long orderId, @Param("salesItemId") Long salesItemId);

}
