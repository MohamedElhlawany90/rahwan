//package com.blueWhale.Rahwan.order.service;
//
//import com.blueWhale.Rahwan.mapconfig.GoogleMapsProperties;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//@Service
//@RequiredArgsConstructor
//public class GoogleMapsDistanceService {
//
//    private final RestTemplate restTemplate;
//    private final GoogleMapsProperties googleMapsProperties;
//
//    public double calculateDrivingDistanceKm(
//            double originLat, double originLng,
//            double destLat, double destLng
//    ) {
//
//        String url = String.format(
//                "https://maps.googleapis.com/maps/api/directions/json?origin=%f,%f&destination=%f,%f&mode=driving&key=%s",
//                originLat, originLng,
//                destLat, destLng,
//                googleMapsProperties.getApiKey()
//        );
//
//        GoogleDirectionsResponse response =
//                restTemplate.getForObject(url, GoogleDirectionsResponse.class);
//
//        if (response == null || response.getRoutes() == null || response.getRoutes().isEmpty()) {
//            throw new RuntimeException("Google Maps returned no routes");
//        }
//
//        GoogleDirectionsResponse.Route route = response.getRoutes().get(0);
//
//        if (route.getLegs() == null || route.getLegs().isEmpty()) {
//            throw new RuntimeException("Google Maps returned no legs");
//        }
//
//        GoogleDirectionsResponse.Leg leg = route.getLegs().get(0);
//
//        long distanceMeters = leg.getDistance().getValue();
//
//        double distanceKm = distanceMeters / 1000.0;
//
//        return round(distanceKm);
//    }
//
//    private double round(double value) {
//        return Math.round(value * 100.0) / 100.0;
//    }
//}
