package com.blueWhale.Rahwan.order.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CostCalculationService {

    private static final double     BASE_COST = 20.0;
    private static final double COST_PER_KM = 2.5;
//    private static final double INSURANCE_RATE = 0.01; // 1%
    private static final double ROAD_MULTIPLIER = 1.2;

    @Autowired
    private DistanceService distanceService;

    public PricingDetails calculateCost(
            double pickupLat, double pickupLng,
            double recipientLat, double recipientLng,
            Double insuranceValue) {

        // 1️⃣ حساب المسافة
        double rawDistance = distanceService.calculateDistance(
                pickupLat, pickupLng,
                recipientLat, recipientLng
        );

        double adjustedDistance = round(
                rawDistance * ROAD_MULTIPLIER
        );

        // 2️⃣ تكلفة المسافة
        double distanceCost = round(
                adjustedDistance * COST_PER_KM
        );
//
//        // 3️⃣ تكلفة التأمين
//        double insuranceCost =
//                insuranceValue != null && insuranceValue > 0
//                        ? round(insuranceValue * INSURANCE_RATE)
//                        : 0.0;

        // 4️⃣ الإجمالي
        double totalCost = round(
                BASE_COST + distanceCost
//                        + insuranceCost
        );

        // 5️⃣ Breakdown
        return PricingDetails.builder()
                .baseCost(BASE_COST)
                .distanceCost(distanceCost)
//                .insuranceCost(insuranceCost)
                .totalCost(totalCost)
                .distanceKm(adjustedDistance)
                .distanceDisplay(adjustedDistance + " km")
                .build();
    }

    // 🔧 تقريب رقمين عشريين
    private double round(double value) {

        return Math.round(value * 100.0) / 100.0;
    }
}
