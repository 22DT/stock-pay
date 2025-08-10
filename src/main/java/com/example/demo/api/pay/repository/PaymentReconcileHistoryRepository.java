package com.example.demo.api.pay.repository;


import com.example.demo.api.pay.entity.PaymentReconcileHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentReconcileHistoryRepository extends JpaRepository<PaymentReconcileHistory, Long> {
}
