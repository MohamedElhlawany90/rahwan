package com.blueWhale.Rahwan.order.service;

import com.blueWhale.Rahwan.order.dto.DistanceResult;
import com.blueWhale.Rahwan.order.service.distance.DistanceCalculationStrategy;
import com.blueWhale.Rahwan.order.service.distance.GoogleMapsDistanceStrategy;
import com.blueWhale.Rahwan.order.service.distance.HaversineDistanceStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * خدمة حساب المسافة مع دعم استراتيجيات متعددة و fallback تلقائي
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistanceService {

    private final GoogleMapsDistanceStrategy googleMapsStrategy;
    private final HaversineDistanceStrategy haversineStrategy;

    @Value("${distance.calculation.fallback-enabled:true}")
    private boolean fallbackEnabled;

    /**
     * حساب المسافة مع استخدام Google Maps و fallback لـ Haversine
     */
    public DistanceResult calculateDistanceWithFallback(
            double lat1, double lon1, double lat2, double lon2) {

        log.info("Starting distance calculation from ({},{}) to ({},{})", lat1, lon1, lat2, lon2);

        // محاولة استخدام Google Maps أولاً
        if (googleMapsStrategy.isAvailable()) {
            try {
                DistanceResult result = googleMapsStrategy.calculateDistance(lat1, lon1, lat2, lon2);
                log.info("Successfully calculated distance using Google Maps: {} km", result.getDistanceKm());
                return result;
            } catch (Exception e) {
                log.error("Google Maps calculation failed: {}", e.getMessage());

                if (fallbackEnabled) {
                    log.info("Falling back to Haversine calculation");
                    return useFallback(lat1, lon1, lat2, lon2);
                } else {
                    throw new RuntimeException("Distance calculation failed and fallback is disabled", e);
                }
            }
        } else {
            log.warn("Google Maps API is not configured, using fallback method");
            return useFallback(lat1, lon1, lat2, lon2);
        }
    }

    /**
     * حساب المسافة باستخدام Google Maps فقط (بدون fallback)
     */
    public DistanceResult calculateDistanceGoogleMapsOnly(
            double lat1, double lon1, double lat2, double lon2) {

        if (!googleMapsStrategy.isAvailable()) {
            throw new IllegalStateException("Google Maps API is not configured");
        }

        return googleMapsStrategy.calculateDistance(lat1, lon1, lat2, lon2);
    }

    /**
     * حساب المسافة باستخدام Haversine (للاختبار أو الحالات الخاصة)
     */
    public DistanceResult calculateDistanceHaversine(
            double lat1, double lon1, double lat2, double lon2) {

        return haversineStrategy.calculateDistance(lat1, lon1, lat2, lon2);
    }

    /**
     * استخدام طريقة الـ fallback
     */
    private DistanceResult useFallback(double lat1, double lon1, double lat2, double lon2) {
        try {
            DistanceResult result = haversineStrategy.calculateDistance(lat1, lon1, lat2, lon2);
            log.info("Fallback calculation successful: {} km", result.getDistanceKm());
            return result;
        } catch (Exception e) {
            log.error("Fallback calculation also failed: {}", e.getMessage());
            throw new RuntimeException("All distance calculation methods failed", e);
        }
    }

    /**
     * حساب المسافة مع caching (للطلبات المتكررة)
     */
    @Cacheable(value = "distanceCache",
            key = "#lat1 + '_' + #lon1 + '_' + #lat2 + '_' + #lon2",
            unless = "#result == null")
    public DistanceResult calculateDistanceWithCache(
            double lat1, double lon1, double lat2, double lon2) {

        DistanceResult result = calculateDistanceWithFallback(lat1, lon1, lat2, lon2);

        // تعديل المصدر إذا كان من الـ cache
        if (result != null) {
            result.setFromCache(false); // سيتم تعديله بواسطة Spring Cache
        }

        return result;
    }

    /**
     * طريقة للتوافق مع الكود القديم - ترجع المسافة فقط
     * @deprecated استخدم calculateDistanceWithFallback بدلاً منها
     */
    @Deprecated
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        DistanceResult result = calculateDistanceWithFallback(lat1, lon1, lat2, lon2);
        return result.getDistanceKm();
    }

    /**
     * جلب معلومات حالة الخدمة
     */
    public ServiceStatus getServiceStatus() {
        return ServiceStatus.builder()
                .googleMapsAvailable(googleMapsStrategy.isAvailable())
                .haversineAvailable(haversineStrategy.isAvailable())
                .fallbackEnabled(fallbackEnabled)
                .primaryStrategy(googleMapsStrategy.isAvailable() ?
                        "Google Maps" : "Haversine (Fallback)")
                .build();
    }

    @lombok.Data
    @lombok.Builder
    public static class ServiceStatus {
        private boolean googleMapsAvailable;
        private boolean haversineAvailable;
        private boolean fallbackEnabled;
        private String primaryStrategy;
    }
}