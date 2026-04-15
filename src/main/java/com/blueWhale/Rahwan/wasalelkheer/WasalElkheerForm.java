package com.blueWhale.Rahwan.wasalelkheer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WasalElkheerForm {

    @NotNull(message = "User latitude is required")
    private Double userLatitude;

    @NotNull(message = "User longitude is required")
    private Double userLongitude;

    private String address;

    @NotNull(message = "Charity ID is required")
    private Long charityId;

    @NotNull(message = "Order type is required")
    private WasalElkheerType orderType;

    private MultipartFile photo;

    private String additionalNotes;

    @NotNull(message = "Collection date is required")
    private LocalDate collectionDate;

    // ✅ FIX: Was @NotNull on a String, which allows empty strings through validation.
    // Changed to @NotBlank to properly reject null AND empty/blank values.
    // @DateTimeFormat has no effect on String fields — removed it to avoid confusion.
    @NotBlank(message = "Collection time is required")
    @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$", message = "Invalid time format, expected HH:mm")
    private String collectionTime;

    // ✅ Helper used by service to get LocalTime safely
    public LocalTime getParsedCollectionTime() {
        return LocalTime.parse(collectionTime);
    }

    private boolean anyTime = false;

    private boolean allowInspection = false;

    private boolean shippingPaidByReceiver = false;
}