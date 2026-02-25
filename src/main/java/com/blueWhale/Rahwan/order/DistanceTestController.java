package com.blueWhale.Rahwan.order;

import com.blueWhale.Rahwan.order.dto.DistanceResult;
import com.blueWhale.Rahwan.order.service.CostCalculationService;
import com.blueWhale.Rahwan.order.service.DistanceService;
import com.blueWhale.Rahwan.order.service.PricingDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller لاختبار حساب المسافة والتكلفة
 */
@RestController
@RequestMapping("/api/test/distance")
@RequiredArgsConstructor
public class DistanceTestController {

    private final DistanceService distanceService;
    private final CostCalculationService costCalculationService;

    /**
     * اختبار حساب المسافة مع fallback
     */
    @GetMapping("/calculate")
    public ResponseEntity<DistanceResult> testDistance(
            @RequestParam double lat1,
            @RequestParam double lon1,
            @RequestParam double lat2,
            @RequestParam double lon2) {

        DistanceResult result = distanceService.calculateDistanceWithFallback(lat1, lon1, lat2, lon2);
        return ResponseEntity.ok(result);
    }

    /**
     * اختبار حساب المسافة باستخدام Google Maps فقط
     */
    @GetMapping("/calculate/google-only")
    public ResponseEntity<DistanceResult> testDistanceGoogleOnly(
            @RequestParam double lat1,
            @RequestParam double lon1,
            @RequestParam double lat2,
            @RequestParam double lon2) {

        try {
            DistanceResult result = distanceService.calculateDistanceGoogleMapsOnly(lat1, lon1, lat2, lon2);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    DistanceResult.builder()
                            .message("Error: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * اختبار حساب المسافة باستخدام Haversine
     */
    @GetMapping("/calculate/haversine")
    public ResponseEntity<DistanceResult> testDistanceHaversine(
            @RequestParam double lat1,
            @RequestParam double lon1,
            @RequestParam double lat2,
            @RequestParam double lon2) {

        DistanceResult result = distanceService.calculateDistanceHaversine(lat1, lon1, lat2, lon2);
        return ResponseEntity.ok(result);
    }

    /**
     * اختبار حساب التكلفة الكاملة
     */
    @GetMapping("/calculate/cost")
    public ResponseEntity<PricingDetails> testCost(
            @RequestParam double pickupLat,
            @RequestParam double pickupLng,
            @RequestParam double recipientLat,
            @RequestParam double recipientLng) {

        PricingDetails result = costCalculationService.calculateCost(
                pickupLat, pickupLng, recipientLat, recipientLng, null
        );
        return ResponseEntity.ok(result);
    }

    /**
     * جلب حالة الخدمة
     */
    @GetMapping("/status")
    public ResponseEntity<DistanceService.ServiceStatus> getServiceStatus() {
        return ResponseEntity.ok(distanceService.getServiceStatus());
    }

    /**
     * مقارنة بين Google Maps و Haversine
     */
    @GetMapping("/compare")
    public ResponseEntity<ComparisonResult> compareStrategies(
            @RequestParam double lat1,
            @RequestParam double lon1,
            @RequestParam double lat2,
            @RequestParam double lon2) {

        ComparisonResult comparison = new ComparisonResult();

        try {
            comparison.googleMaps = distanceService.calculateDistanceGoogleMapsOnly(lat1, lon1, lat2, lon2);
        } catch (Exception e) {
            comparison.googleMapsError = e.getMessage();
        }

        comparison.haversine = distanceService.calculateDistanceHaversine(lat1, lon1, lat2, lon2);

        if (comparison.googleMaps != null && comparison.haversine != null) {
            comparison.difference = Math.abs(
                    comparison.googleMaps.getDistanceKm() - comparison.haversine.getDistanceKm()
            );
            comparison.percentageDifference = (comparison.difference / comparison.haversine.getDistanceKm()) * 100;
        }

        return ResponseEntity.ok(comparison);
    }

    @lombok.Data
    private static class ComparisonResult {
        private DistanceResult googleMaps;
        private DistanceResult haversine;
        private String googleMapsError;
        private Double difference;
        private Double percentageDifference;
    }
}