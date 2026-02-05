package com.blueWhale.Rahwan.pricing;


import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingSettingsDto {

    private Long id;
    private String settingKey;
    private double baseCost;
    private double costPerKm;
    private double roadMultiplier;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}