package com.blueWhale.Rahwan.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /** All transactions where the user sent or received money — full wallet history. */
    @Query("""
        SELECT t FROM Transaction t
        WHERE t.fromUserId = :userId OR t.toUserId = :userId
        ORDER BY t.createdAt DESC
    """)
    List<Transaction> findWalletHistory(@Param("userId") UUID userId);

    /** All transactions tied to a specific order. */
    List<Transaction> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    /** Outgoing transfers only (debits). */
    List<Transaction> findByFromUserIdOrderByCreatedAtDesc(UUID fromUserId);

    /** Incoming transfers only (credits). */
    List<Transaction> findByToUserIdOrderByCreatedAtDesc(UUID toUserId);
}