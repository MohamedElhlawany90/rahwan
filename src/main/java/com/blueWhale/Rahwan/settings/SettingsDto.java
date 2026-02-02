package com.blueWhale.Rahwan.settings;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SettingsDto {

    private String id;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    private String createdBy;
    private Long updatedBy;

    private String termsAndConditions;
    private String shippingTerms;
    private String requestType;
    private String rejectReason;
    private String contactUs;
}
