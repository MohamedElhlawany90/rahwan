package com.blueWhale.Rahwan.wasalelkheer;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WasalElkheerDto {

    private Long id;
    private UUID userId;
    private String userName;
    private Long charityId;
    private String charityNameAr;
    private String charityNameEn;
    private Double userLatitude;
    private Double userLongitude;
    private String address;
    private WasalElkheerType orderType;
    private String photo;
    private String additionalNotes;
    private LocalDate collectionDate;
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime collectionTime;
    private boolean anyTime;
    private boolean allowInspection;
    private boolean shippingPaidByReceiver;
    private WasalElkheerStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}