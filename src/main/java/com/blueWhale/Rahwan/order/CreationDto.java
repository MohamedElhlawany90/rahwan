package com.blueWhale.Rahwan.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreationDto {
    private Long id;
    private UUID userId;
    private UUID driverId;
    private String userName;
    private String driverName;

    // Pickup Location
    private double pickupLatitude;
    private double pickupLongitude;
    private String pickupAddress;

    // Recipient Location
    private double recipientLatitude;
    private double recipientLongitude;
    private String recipientAddress;

    // Recipient Details
    private String recipientName;
    private String recipientPhone;

    // Order Details
    private OrderType orderType;
    private double insuranceValue;
    private double deliveryCost;
    private String pictureUrl;
    private String additionalNotes;

    // Collection Time
    private LocalDate collectionDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime collectionTime;

    private Boolean anyTime;

    // Options
    private Boolean allowInspection;
    private Boolean receiverPaysShipping;

    // Status
    private CreationStatus status ;
    private String trackingNumber;
    private double distanceKm;

    // OTP Status (don't expose actual OTPs)
    private boolean pickupConfirmed;
    private boolean deliveryConfirmed;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;
}