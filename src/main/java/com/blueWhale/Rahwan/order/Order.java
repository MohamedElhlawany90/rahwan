package com.blueWhale.Rahwan.order;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Category ──────────────────────────────────────────────────────────
    /**
     * Determines whether this is a regular delivery or a charity donation (WasalElkheer).
     * Drives all branching logic in OrderService.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderCategory orderCategory = OrderCategory.REGULAR;

    // ── Shared: Sender ────────────────────────────────────────────────────
    @Column(nullable = false)
    private UUID userId;

    private UUID driverId;

    private String photo;
    private String driverPhoto;

    // ── REGULAR only: Pickup Location ─────────────────────────────────────
    private double pickupLatitude;
    private double pickupLongitude;
    private String pickupAddress;

    // ── REGULAR only: Recipient Location & Details ────────────────────────
    private double recipientLatitude;
    private double recipientLongitude;
    private String recipientAddress;
    private String recipientName;
    private String recipientPhone;

    // ── CHARITY only: Donor pickup location ──────────────────────────────
    // Reuses pickupLatitude/pickupLongitude/pickupAddress for the donor's location.
    // charityId holds the selected charity; charity coordinates are looked up at
    // runtime (not stored) to keep charity data consistent with CharityRepository.

    /**
     * CHARITY orders only. The ID of the selected Charity (recipient).
     * NULL for REGULAR orders.
     */
    private Long charityId;

    // ── Shared: Order Details ─────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType orderType;

    private double insuranceValue;

    @Column(nullable = false)
    private double deliveryCost;

    // ── REGULAR only: Commission ──────────────────────────────────────────
    // For CHARITY orders these remain 0.0 — the app pays full deliveryCost to driver.
    @Column(nullable = false)
    private double commissionRate = 0.0;

    @Column(nullable = false)
    private double appCommission = 0.0;

    @Column(nullable = false)
    private double driverEarnings = 0.0;

    @Column(length = 500)
    private String additionalNotes;

    private String rejectionReason;

    // ── Shared: Collection Time ───────────────────────────────────────────
    private LocalDate collectionDate;
    private LocalTime collectionTime;

    @Column(nullable = false)
    private Boolean anyTime = false;

    @Column(nullable = false)
    private Boolean allowInspection = false;

    @Column(nullable = false)
    private Boolean receiverPaysShipping = false;

    // ── Shared: Status & Tracking ─────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CreationStatus creationStatus;

    @Column(unique = true)
    private String trackingNumber;

    private double distanceKm;

    // ── Shared: OTP ───────────────────────────────────────────────────────
    private String otpForPickup;
    private String otpForDelivery;
    private boolean pickupConfirmed = false;
    private boolean deliveryConfirmed = false;

    // REGULAR only — CHARITY never has a return flow
    private String otpForReturn;

    // ── Shared: Timestamps ────────────────────────────────────────────────
    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}