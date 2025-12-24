package com.blueWhale.Rahwan.order.service;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostBreakdown {
    private double baseCost;
    private double distanceCost;
    private double insuranceCost;
    private double totalCost;
    private String distanceDisplay;
    private double distanceKm;
}