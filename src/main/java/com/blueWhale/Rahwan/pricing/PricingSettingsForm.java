package com.blueWhale.Rahwan.pricing;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingSettingsForm {

    @NotNull(message = "Base cost is required")
    @Min(value = 0, message = "Base cost must be positive")
    private Double baseCost;

    @NotNull(message = "Cost per km is required")
    @Min(value = 0, message = "Cost per km must be positive")
    private Double costPerKm;

    @NotNull(message = "Road multiplier is required")
    @Min(value = 1, message = "Road multiplier must be at least 1.0")
    private Double roadMultiplier;
}