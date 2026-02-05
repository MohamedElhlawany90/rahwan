package com.blueWhale.Rahwan.order;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDto {

    private Long id;
    private UUID userId;
    private String userName;
    private UUID driverId;
    private String driverName;
    private String photo;

    // Locations
    private double pickupLatitude;
    private double pickupLongitude;
    private String pickupAddress;
    private double recipientLatitude;
    private double recipientLongitude;
    private String recipientAddress;

    // Recipient
    private String recipientName;
    private String recipientPhone;

    // Order Details
    private OrderType orderType;
    private double insuranceValue;
    private double deliveryCost;

    // Commission Details
    private double commissionRate;
    private double appCommission;
    private double driverEarnings;

    private String additionalNotes;
    private String rejectionReason;

    // Collection Time
    private LocalDate collectionDate;
    private LocalTime collectionTime;
    private Boolean anyTime;
    private Boolean allowInspection;
    private Boolean receiverPaysShipping;

    // Status
    private OrderStatus status;
    private CreationStatus creationStatus;
    private String trackingNumber;
    private double distanceKm;

    // OTP
    private String otpForPickup;
    private String otpForDelivery;
    private boolean pickupConfirmed;
    private boolean deliveryConfirmed;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;
}