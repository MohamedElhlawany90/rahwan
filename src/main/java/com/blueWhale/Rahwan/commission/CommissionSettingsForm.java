package com.blueWhale.Rahwan.commission;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionSettingsForm {

    @NotNull(message = "Commission rate is required")
    @Min(value = 0, message = "Commission rate must be at least 0%")
    @Max(value = 100, message = "Commission rate cannot exceed 100%")
    private Double commissionRate;
}