package com.example.demo.api.pay.repository;


import com.example.demo.api.pay.entity.PaymentCancelHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentCancelHistoryRepository extends JpaRepository<PaymentCancelHistory, Long> {
}
