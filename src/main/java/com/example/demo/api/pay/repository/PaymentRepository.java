package com.example.demo.api.pay.repository;

import com.example.demo.api.pay.entity.Payment;
import com.example.demo.api.pay.enums.PaymentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("select p from Payment p" +
            " where p.paymentKey=:paymentKey")
    Optional<Payment> findByPaymentKey(@Param("paymentKey")String paymentKey);

    @Query("select p from Payment p" +
            " where p.paymentStatus in :statuses and p.createdDate <= :beforeDate")
    List<Payment> findPaymentsCreatedBefore(@Param("statuses")List<PaymentStatus> statuses, @Param("beforeDate") LocalDate beforeDate, Pageable pageable);

    @Query("select p from Payment p" +
            " where p.paymentStatus = :status and p.createdDate <= :beforeDate")
    List<Payment> findPaymentCreatedBefore(@Param("status")PaymentStatus status, @Param("beforeDate") LocalDate beforeDate, Pageable pageable);


    @Transactional
    @Modifying
    @Query("update Payment p set p.paymentStatus=:paymentStatus where p.paymentKey=:paymentKey")
    void updatePaymentStatusByPaymentKey(@Param("paymentKey") String paymentKey, @Param("paymentStatus") PaymentStatus PaymentStatus);

}
