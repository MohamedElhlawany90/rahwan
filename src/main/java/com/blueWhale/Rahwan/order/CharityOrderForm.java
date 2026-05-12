package com.blueWhale.Rahwan.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Form for creating / updating a CHARITY order (WasalElkheer donation delivery).
 *
 * Key differences from OrderForm:
 *  - Has charityId instead of recipient fields (charity is always the receiver).
 *  - Has userLatitude/userLongitude instead of pickupLatitude/pickupLongitude
 *    (clearer naming for the donor's location; mapped to pickup fields on the entity).
 *  - No insuranceValue — no financial liability on driver for charity orders.
 *  - No receiverPaysShipping — app always pays.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharityOrderForm {

    @NotNull(message = "Charity ID is required")
    private Long charityId;

    @NotNull(message = "User latitude is required")
    private Double userLatitude;

    @NotNull(message = "User longitude is required")
    private Double userLongitude;

    private String address;

    @NotNull(message = "Order type is required")
    private OrderType orderType;

    private MultipartFile photo;

    private String additionalNotes;

    @NotNull(message = "Collection date is required")
    private LocalDate collectionDate;

    @NotBlank(message = "Collection time is required")
    @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$", message = "Invalid time format, expected HH:mm")
    private String collectionTime;

//    /** Helper used by service to parse the time string safely. */
//    public LocalTime getParsedCollectionTime() {
//        return LocalTime.parse(collectionTime);
//    }

    @NotNull(message = "Any time flag is required")
    private boolean anyTime = false;

    @NotNull(message = "Allow inspection flag is required")
    private boolean allowInspection = false;
}