package com.blueWhale.Rahwan.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);
    Optional<Order> findByTrackingNumber(String trackingNumber);
    List<Order> findByDriverIdOrderByCreatedAtDesc(UUID driverId);
    List<Order> findByUserId(UUID userId);
//    List<Order> findByUserIdAndStatusNotOrderByCreatedAtDesc(UUID userId, OrderStatus status);
//    List<Order> findByDriverIdAndStatusNotOrderByCreatedAtDesc(UUID driverId, OrderStatus status);
//    List<Order> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, OrderStatus status);

    @Query("""
        SELECT o
        FROM Order o
        WHERE o.status = :status
          AND o.deliveredAt IS NOT NULL
          AND o.deliveredAt BETWEEN :startDate AND :endDate
    """)
    List<Order> findDeliveredOrdersBetweenDates(
            @Param("status") OrderStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}