package com.blueWhale.Rahwan.orderorg;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderOrgDto {

    private Long id;
    private UUID userId;
    private String userName;
    private Long charityId;
    private String charityNameAr;
    private String charityNameEn;
    private Double userLatitude;
    private Double userLongitude;
    private OrderOrgType orderType;
    private String photo;
    private String additionalNotes;
    private LocalDate collectionDate;
    private LocalTime collectionTime;
    private boolean anyTime;
    private boolean allowInspection;
    private boolean shippingPaidByReceiver;
    private OrderOrgStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}