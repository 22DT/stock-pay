package com.example.demo.api.item.repository;

import com.example.demo.api.item.entity.StockHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {

}
