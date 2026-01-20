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

    @Column(nullable = false)
    private UUID userId;

    private UUID driverId;

    private String photo ;

    // Pickup Location
    private double pickupLatitude;
    private double pickupLongitude;
    private String pickupAddress;

    // Recipient Location
    private double recipientLatitude;
    private double recipientLongitude;
    private String recipientAddress;

    // Recipient Details
    @Column(nullable = false)
    private String recipientName;
    @Column(nullable = false)
    private String recipientPhone;

    // Order Details
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType orderType;

    private double insuranceValue;

    @Column(nullable = false)
    private double deliveryCost;

    @Column(length = 500)
    private String additionalNotes;

    private String rejectionReason;

    // Collection Time
    private LocalDate collectionDate;
    private LocalTime collectionTime;
    @Column(nullable = false)
    private Boolean anyTime = false;

    // Options
    @Column(nullable = false)
    private Boolean allowInspection = false;
    @Column(nullable = false)
    private Boolean receiverPaysShipping = false;

    // Status & Tracking
    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
    private OrderStatus status ;

    // creation status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CreationStatus creationStatus ;

    @Column(unique = true)
    private String trackingNumber;

    // Distance
    private double distanceKm;

    // OTP Fields
    private String otpForPickup;
    private String otpForDelivery;
    private boolean pickupConfirmed = false;
    private boolean deliveryConfirmed = false;

    // Timestamps
    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
////        if (status == null) {
////            status = OrderStatus.PENDING;
////        }
  }
}