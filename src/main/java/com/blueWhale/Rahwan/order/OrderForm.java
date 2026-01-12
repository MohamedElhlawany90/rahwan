package com.blueWhale.Rahwan.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderForm {

    private MultipartFile photo ;

    @NotNull(message = "Pickup latitude is required")
    private double pickupLatitude;

    @NotNull(message = "Pickup longitude is required")
    private double pickupLongitude;

    @NotBlank(message = "Pickup address is required")
    private String pickupAddress;

    @NotNull(message = "Recipient latitude is required")
    private double recipientLatitude;

    @NotNull(message = "Recipient longitude is required")
    private double recipientLongitude;

    @NotBlank(message = "Recipient address is required")
    private String recipientAddress;

    @NotBlank(message = "Recipient name is required")
    private String recipientName;

    @NotBlank(message = "Recipient phone is required")
    @Pattern(regexp = "^20\\d{10}$", message = "Phone must start with 20 and be 12 digits")
    private String recipientPhone;

    @NotNull(message = "Order type is required")
    private OrderType orderType;

    @DecimalMin(value = "0.0", message = "Insurance value must be positive")
    private double insuranceValue;

    @Size(max = 500, message = "Additional notes must not exceed 500 characters")
    private String additionalNotes;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate collectionDate;

    @DateTimeFormat(pattern = "HH:mm")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime collectionTime;

    @NotNull(message = "Any time flag is required")
    private Boolean anyTime = false;

    @NotNull(message = "Allow inspection flag is required")
    private Boolean allowInspection = false;

    @NotNull(message = "Receiver pays shipping flag is required")
    private Boolean receiverPaysShipping = false;
}