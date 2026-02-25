package com.blueWhale.Rahwan.order.service;

import com.blueWhale.Rahwan.order.dto.DistanceResult;
import com.blueWhale.Rahwan.pricing.PricingSettings;
import com.blueWhale.Rahwan.pricing.PricingSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CostCalculationService {

    private final DistanceService distanceService;
    private final PricingSettingsService pricingSettingsService;

    /**
     * حساب التكلفة مع استخدام Google Maps والـ fallback التلقائي
     */
    public PricingDetails calculateCost(
            double pickupLat, double pickupLng,
            double recipientLat, double recipientLng,
            Double insuranceValue) {

        log.info("Calculating cost for delivery from ({},{}) to ({},{})",
                pickupLat, pickupLng, recipientLat, recipientLng);

        // جلب الإعدادات النشطة
        PricingSettings settings = pricingSettingsService.getActiveSettings();

        // 1️⃣ حساب المسافة باستخدام النظام الجديد (Google Maps + fallback)
        DistanceResult distanceResult = distanceService.calculateDistanceWithFallback(
                pickupLat, pickupLng,
                recipientLat, recipientLng
        );

        double rawDistance = distanceResult.getDistanceKm();

        // تطبيق road multiplier (فقط إذا كانت النتيجة من Haversine)
        double adjustedDistance;
        if (distanceResult.getSource() == DistanceResult.DistanceSource.HAVERSINE_FALLBACK) {
            // Haversine يحتاج road multiplier لأنه خط مستقيم
            adjustedDistance = round(rawDistance * settings.getRoadMultiplier());
            log.info("Applied road multiplier {} to Haversine distance: {} -> {} km",
                    settings.getRoadMultiplier(), rawDistance, adjustedDistance);
        } else {
            // Google Maps بالفعل يحسب المسافة على الطرق الفعلية
            adjustedDistance = round(rawDistance);
            log.info("Using Google Maps distance without multiplier: {} km", adjustedDistance);
        }

        // 2️⃣ تكلفة المسافة
        double distanceCost = round(adjustedDistance * settings.getCostPerKm());

        // 3️⃣ الإجمالي
        double totalCost = round(settings.getBaseCost() + distanceCost);

        log.info("Cost calculation complete: base={}, distance={} km, distanceCost={}, total={}",
                settings.getBaseCost(), adjustedDistance, distanceCost, totalCost);

        // 4️⃣ Breakdown
        return PricingDetails.builder()
                .baseCost(settings.getBaseCost())
                .costPerKm(settings.getCostPerKm())
                .roadMultiplier(settings.getRoadMultiplier())
                .distanceCost(distanceCost)
                .totalCost(totalCost)
                .distanceKm(adjustedDistance)
                .distanceDisplay(adjustedDistance + " km")
                .estimatedDuration(distanceResult.getDurationMinutes())
                .distanceSource(distanceResult.getSource().name())
                .calculationNotes(distanceResult.getMessage())
                .build();
    }

    /**
     * حساب التكلفة باستخدام Google Maps فقط (بدون fallback)
     * للحالات التي تريد ضمان استخدام Google Maps فقط
     */
    public PricingDetails calculateCostGoogleMapsOnly(
            double pickupLat, double pickupLng,
            double recipientLat, double recipientLng,
            Double insuranceValue) {

        PricingSettings settings = pricingSettingsService.getActiveSettings();

        // استخدام Google Maps فقط
        DistanceResult distanceResult = distanceService.calculateDistanceGoogleMapsOnly(
                pickupLat, pickupLng,
                recipientLat, recipientLng
        );

        double adjustedDistance = round(distanceResult.getDistanceKm());
        double distanceCost = round(adjustedDistance * settings.getCostPerKm());
        double totalCost = round(settings.getBaseCost() + distanceCost);

        return PricingDetails.builder()
                .baseCost(settings.getBaseCost())
                .costPerKm(settings.getCostPerKm())
                .roadMultiplier(settings.getRoadMultiplier())
                .distanceCost(distanceCost)
                .totalCost(totalCost)
                .distanceKm(adjustedDistance)
                .distanceDisplay(adjustedDistance + " km")
                .estimatedDuration(distanceResult.getDurationMinutes())
                .distanceSource(distanceResult.getSource().name())
                .build();
    }

    // 🔧 تقريب رقمين عشريين
    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}