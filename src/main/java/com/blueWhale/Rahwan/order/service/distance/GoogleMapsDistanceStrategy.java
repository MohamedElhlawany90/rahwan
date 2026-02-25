package com.blueWhale.Rahwan.order.service.distance;

import com.blueWhale.Rahwan.config.GoogleMapsConfig;
import com.blueWhale.Rahwan.order.dto.DistanceResult;
import com.blueWhale.Rahwan.order.dto.GoogleMapsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * حساب المسافة باستخدام Google Maps Distance Matrix API
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleMapsDistanceStrategy implements DistanceCalculationStrategy {

    private final GoogleMapsConfig config;
    private final RestTemplate restTemplate;

    @Override
    public DistanceResult calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        try {
            log.debug("Calculating distance using Google Maps API from ({},{}) to ({},{})",
                    lat1, lon1, lat2, lon2);

            String url = buildUrl(lat1, lon1, lat2, lon2);

            GoogleMapsDto.DistanceMatrixResponse response = restTemplate.getForObject(
                    url,
                    GoogleMapsDto.DistanceMatrixResponse.class
            );

            if (response == null || !"OK".equals(response.getStatus())) {
                log.error("Google Maps API returned invalid response: {}",
                        response != null ? response.getStatus() : "null");
                throw new RuntimeException("Invalid Google Maps API response");
            }

            GoogleMapsDto.Element element = response.getRows().get(0).getElements().get(0);

            if (!"OK".equals(element.getStatus())) {
                log.error("Google Maps API element status: {}", element.getStatus());
                throw new RuntimeException("Distance calculation failed: " + element.getStatus());
            }

            // تحويل من متر إلى كيلومتر
            double distanceKm = element.getDistance().getValue() / 1000.0;

            // تحويل من ثواني إلى دقائق
            int durationMinutes = element.getDuration().getValue() / 60;

            log.info("Google Maps distance calculated: {} km, {} minutes", distanceKm, durationMinutes);

            return DistanceResult.builder()
                    .distanceKm(round(distanceKm))
                    .durationMinutes(durationMinutes)
                    .source(DistanceResult.DistanceSource.GOOGLE_MAPS)
                    .fromCache(false)
                    .build();

        } catch (Exception e) {
            log.error("Error calculating distance with Google Maps: {}", e.getMessage(), e);
            throw new RuntimeException("Google Maps API error", e);
        }
    }

    private String buildUrl(double lat1, double lon1, double lat2, double lon2) {
        return UriComponentsBuilder
                .fromHttpUrl(config.getBaseUrl() + "/distancematrix/json")
                .queryParam("origins", lat1 + "," + lon1)
                .queryParam("destinations", lat2 + "," + lon2)
                .queryParam("key", config.getKey())
                .queryParam("mode", "driving")  // يمكن تغييره حسب الحاجة
                .queryParam("language", "ar")    // اللغة العربية
                .toUriString();
    }

    @Override
    public String getStrategyName() {
        return "Google Maps Distance Matrix API";
    }

    @Override
    public boolean isAvailable() {
        return config.getKey() != null && !config.getKey().isEmpty();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}