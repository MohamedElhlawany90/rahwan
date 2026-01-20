package com.blueWhale.Rahwan.orderorg;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderOrgForm {

    @NotNull(message = "User latitude is required")
    private Double userLatitude;

    @NotNull(message = "User longitude is required")
    private Double userLongitude;

    @NotNull(message = "Charity ID is required")
    private Long charityId;

    @NotNull(message = "Order type is required")
    private OrderOrgType orderType;

    private MultipartFile photo;

    private String additionalNotes;

    @NotNull(message = "Collection date is required")
    private LocalDate collectionDate;

    @NotNull(message = "Collection time is required")
    @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$", message = "Invalid time format")
    private LocalTime collectionTime;

    private boolean anyTime = false;

    private boolean allowInspection = false;

    private boolean shippingPaidByReceiver = false;
}