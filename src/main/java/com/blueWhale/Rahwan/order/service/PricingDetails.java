package com.blueWhale.Rahwan.order.service;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingDetails {
    private double baseCost;
    private double costPerKm;
    private double roadMultiplier;
    private double distanceCost;
    private double totalCost;
    private String distanceDisplay;
    private double distanceKm;
}