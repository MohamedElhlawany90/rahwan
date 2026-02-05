package com.blueWhale.Rahwan.order.service;

import com.blueWhale.Rahwan.pricing.PricingSettings;
import com.blueWhale.Rahwan.pricing.PricingSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CostCalculationService {

    private final DistanceService distanceService;
    private final PricingSettingsService pricingSettingsService;

    public PricingDetails calculateCost(
            double pickupLat, double pickupLng,
            double recipientLat, double recipientLng,
            Double insuranceValue) {

        // جلب الإعدادات النشطة
        PricingSettings settings = pricingSettingsService.getActiveSettings();

        // 1️⃣ حساب المسافة
        double rawDistance = distanceService.calculateDistance(
                pickupLat, pickupLng,
                recipientLat, recipientLng
        );

        double adjustedDistance = round(
                rawDistance * settings.getRoadMultiplier()
        );

        // 2️⃣ تكلفة المسافة
        double distanceCost = round(
                adjustedDistance * settings.getCostPerKm()
        );

        // 3️⃣ الإجمالي
        double totalCost = round(
                settings.getBaseCost() + distanceCost
        );

        // 4️⃣ Breakdown
        return PricingDetails.builder()
                .baseCost(settings.getBaseCost())
                .costPerKm(settings.getCostPerKm())
                .roadMultiplier(settings.getRoadMultiplier())
                .distanceCost(distanceCost)
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