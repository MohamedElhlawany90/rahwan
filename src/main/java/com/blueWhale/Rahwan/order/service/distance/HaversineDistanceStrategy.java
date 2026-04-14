package com.blueWhale.Rahwan.order.service.distance;

import com.blueWhale.Rahwan.order.dto.DistanceResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * حساب المسافة باستخدام Haversine formula (خط مستقيم)
 * يستخدم كـ fallback في حال فشل Google Maps
 */
@Component
@Slf4j
public class HaversineDistanceStrategy implements DistanceCalculationStrategy {

    private static final int EARTH_RADIUS_KM = 6371;

    @Override
    public DistanceResult calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        log.debug("Calculating distance using Haversine formula from ({},{}) to ({},{})",
                lat1, lon1, lat2, lon2);

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS_KM * c;

        double roundedDistance = round(distance);

        log.info("Haversine distance calculated: {} km (straight line)", roundedDistance);

        return DistanceResult.builder()
                .distanceKm(roundedDistance)
                .durationMinutes(null) // Haversine لا يحسب الوقت
                .source(DistanceResult.DistanceSource.HAVERSINE_FALLBACK)
                .fromCache(false)
                .message("Calculated using straight-line distance (fallback method)")
                .build();
    }

    @Override
    public String getStrategyName() {
        return "Haversine Formula (Fallback)";
    }

    @Override
    public boolean isAvailable() {
        return true; // دائماً متاح
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}