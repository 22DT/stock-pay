package com.example.demo.api.order.repository;

import com.example.demo.api.order.entity.OrderItem;
import com.example.demo.api.order.enums.OrderItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {


    @Query("select oi from OrderItem oi" +
            " join fetch oi.item" +
            " where oi.order.id=:orderId")
    List<OrderItem> findByOrderIdWithItem(@Param("orderId")Long orderId);

    @Query("select oi from OrderItem oi" +
            " join fetch oi.salesItem" +
            " join fetch oi.item" +
            " where oi.order.id=:orderId")
    List<OrderItem> findByOrderIdWithSalesItemAndItem(@Param("orderId")Long orderId);


    @Modifying @Transactional
    @Query("update OrderItem oi set oi.status = :status where oi.id = :orderItemId")
    void updateStatus(@Param("orderItemId") Long orderItemId, @Param("status") OrderItemStatus status);

    @Modifying @Transactional
    @Query("update OrderItem oi set oi.status = :status where oi.id in :orderItemIds")
    void updateStatus(@Param("orderItemIds") List<Long> orderItemIds, @Param("status") OrderItemStatus status);
}
