package com.blueWhale.Rahwan.transaction;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable ledger record written once per real money-transfer event.
 * Three events trigger a record:
 *   - confirmDelivery   → DELIVERY_COMPLETED
 *   - confirmReturn     → RETURN_PENALTY
 *   - cancelOrderByUser (after driver accepted) → CANCELLATION_COMPENSATION
 */
@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The order that triggered this transfer. */
    @Column(nullable = false)
    private Long orderId;

    /** Human-readable order reference for display. */
    @Column(nullable = false)
    private String trackingNumber;

    /**
     * The wallet that was debited.
     * Null when the app itself is the implicit source (not used currently).
     */
    @Column(nullable = false)
    private UUID fromUserId;

    /** The wallet that was credited. */
    @Column(nullable = false)
    private UUID toUserId;

    /** Net amount transferred (positive, in EGP). */
    @Column(nullable = false)
    private double amount;

    /**
     * For DELIVERY_COMPLETED only: the portion kept by the platform.
     * Zero for RETURN_PENALTY and CANCELLATION_COMPENSATION.
     */
    @Column(nullable = false)
    private double appCommission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    /** Free-text description shown in the wallet history UI. */
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}