package com.blueWhale.Rahwan.order.service;

import com.blueWhale.Rahwan.config.GoogleMapsConfig;
import com.blueWhale.Rahwan.order.dto.DistanceResult;
import com.blueWhale.Rahwan.order.dto.GoogleMapsDto;
import com.blueWhale.Rahwan.order.service.distance.GoogleMapsDistanceStrategy;
import com.blueWhale.Rahwan.order.service.distance.HaversineDistanceStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

/**
 * Unit Tests للـ DistanceService
 *
 * ملحوظة: الـ Tests تستخدم lenient mocking لتجنب UnnecessaryStubbingException
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DistanceServiceTest {

    @Mock
    private GoogleMapsConfig googleMapsConfig;

    @Mock
    private RestTemplate restTemplate;

    private GoogleMapsDistanceStrategy googleMapsStrategy;
    private HaversineDistanceStrategy haversineStrategy;
    private DistanceService distanceService;

    // إحداثيات القاهرة والإسكندرية للاختبار
    private static final double CAIRO_LAT = 30.0444;
    private static final double CAIRO_LNG = 31.2357;
    private static final double ALEXANDRIA_LAT = 31.2001;
    private static final double ALEXANDRIA_LNG = 29.9187;

    @BeforeEach
    void setUp() {
        // Setup Google Maps Config - استخدام lenient للـ mocks
        lenient().when(googleMapsConfig.getKey()).thenReturn("test-api-key");
        lenient().when(googleMapsConfig.getBaseUrl()).thenReturn("https://maps.googleapis.com/maps/api");
        lenient().when(googleMapsConfig.getTimeout()).thenReturn(5000);

        googleMapsStrategy = new GoogleMapsDistanceStrategy(googleMapsConfig, restTemplate);
        haversineStrategy = new HaversineDistanceStrategy();

        // Create service
        distanceService = new DistanceService(googleMapsStrategy, haversineStrategy);

        // تفعيل الـ fallback في الـ service
        ReflectionTestUtils.setField(distanceService, "fallbackEnabled", true);
    }

    @Test
    void testHaversineCalculation() {
        // القاهرة إلى الإسكندرية تقريباً 180 كم خط مستقيم
        DistanceResult result = haversineStrategy.calculateDistance(
                CAIRO_LAT, CAIRO_LNG, ALEXANDRIA_LAT, ALEXANDRIA_LNG
        );

        assertNotNull(result);
        assertEquals(DistanceResult.DistanceSource.HAVERSINE_FALLBACK, result.getSource());
        assertTrue(result.getDistanceKm() > 150 && result.getDistanceKm() < 200,
                "Distance should be between 150-200 km, got: " + result.getDistanceKm());
        assertNull(result.getDurationMinutes()); // Haversine لا يحسب الوقت
    }

    @Test
    void testGoogleMapsCalculation() {
        // Mock Google Maps Response
        GoogleMapsDto.DistanceMatrixResponse mockResponse = createMockGoogleMapsResponse(220000, 7200);

        lenient().when(restTemplate.getForObject(any(String.class), eq(GoogleMapsDto.DistanceMatrixResponse.class)))
                .thenReturn(mockResponse);

        DistanceResult result = googleMapsStrategy.calculateDistance(
                CAIRO_LAT, CAIRO_LNG, ALEXANDRIA_LAT, ALEXANDRIA_LNG
        );

        assertNotNull(result);
        assertEquals(DistanceResult.DistanceSource.GOOGLE_MAPS, result.getSource());
        assertEquals(220.0, result.getDistanceKm()); // 220 km
        assertEquals(120, result.getDurationMinutes()); // 120 minutes
    }

    @Test
    void testDistanceServiceWithSuccessfulGoogleMaps() {
        GoogleMapsDto.DistanceMatrixResponse mockResponse = createMockGoogleMapsResponse(220000, 7200);

        lenient().when(restTemplate.getForObject(any(String.class), eq(GoogleMapsDto.DistanceMatrixResponse.class)))
                .thenReturn(mockResponse);

        DistanceResult result = distanceService.calculateDistanceWithFallback(
                CAIRO_LAT, CAIRO_LNG, ALEXANDRIA_LAT, ALEXANDRIA_LNG
        );

        assertNotNull(result);
        assertEquals(DistanceResult.DistanceSource.GOOGLE_MAPS, result.getSource());
    }

    @Test
    void testDistanceServiceFallbackWhenGoogleMapsFails() {
        // Simulate Google Maps failure
        lenient().when(restTemplate.getForObject(any(String.class), eq(GoogleMapsDto.DistanceMatrixResponse.class)))
                .thenThrow(new RuntimeException("API Error"));

        // Should fallback to Haversine
        DistanceResult result = distanceService.calculateDistanceWithFallback(
                CAIRO_LAT, CAIRO_LNG, ALEXANDRIA_LAT, ALEXANDRIA_LNG
        );

        assertNotNull(result);
        assertEquals(DistanceResult.DistanceSource.HAVERSINE_FALLBACK, result.getSource());
        assertNotNull(result.getMessage());
        assertTrue(result.getMessage().contains("fallback"));
    }

    @Test
    void testShortDistance() {
        // نقطتان قريبتان جداً (1 كم تقريباً)
        double lat1 = 30.0444;
        double lng1 = 31.2357;
        double lat2 = 30.0544;
        double lng2 = 31.2357;

        DistanceResult result = haversineStrategy.calculateDistance(lat1, lng1, lat2, lng2);

        assertNotNull(result);
        assertTrue(result.getDistanceKm() < 2.0,
                "Short distance should be less than 2 km, got: " + result.getDistanceKm());
    }

    @Test
    void testZeroDistance() {
        // نفس النقطة
        DistanceResult result = haversineStrategy.calculateDistance(
                CAIRO_LAT, CAIRO_LNG, CAIRO_LAT, CAIRO_LNG
        );

        assertNotNull(result);
        assertEquals(0.0, result.getDistanceKm());
    }

    @Test
    void testServiceStatus() {
        DistanceService.ServiceStatus status = distanceService.getServiceStatus();

        assertNotNull(status);
        assertTrue(status.isGoogleMapsAvailable()); // مع mock config
        assertTrue(status.isHaversineAvailable());
    }

    // Helper method to create mock Google Maps response
    private GoogleMapsDto.DistanceMatrixResponse createMockGoogleMapsResponse(int distanceMeters, int durationSeconds) {
        GoogleMapsDto.DistanceMatrixResponse response = new GoogleMapsDto.DistanceMatrixResponse();
        response.setStatus("OK");

        GoogleMapsDto.Distance distance = new GoogleMapsDto.Distance();
        distance.setValue(distanceMeters);
        distance.setText((distanceMeters / 1000.0) + " km");

        GoogleMapsDto.Duration duration = new GoogleMapsDto.Duration();
        duration.setValue(durationSeconds);
        duration.setText((durationSeconds / 60) + " mins");

        GoogleMapsDto.Element element = new GoogleMapsDto.Element();
        element.setDistance(distance);
        element.setDuration(duration);
        element.setStatus("OK");

        GoogleMapsDto.Row row = new GoogleMapsDto.Row();
        row.setElements(Collections.singletonList(element));

        response.setRows(Collections.singletonList(row));

        return response;
    }
}