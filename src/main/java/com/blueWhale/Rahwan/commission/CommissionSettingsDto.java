package com.blueWhale.Rahwan.commission;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionSettingsDto {

    private Long id;
    private String settingKey;
    private double commissionRate;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}