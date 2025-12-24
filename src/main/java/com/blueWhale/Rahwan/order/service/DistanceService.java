package com.blueWhale.Rahwan.order.service;

import org.springframework.stereotype.Service;

@Service
public class DistanceService {

    private static final int EARTH_RADIUS_KM = 6371;

    /**
     * حساب المسافة بين نقطتين باستخدام Haversine formula
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS_KM * c;

        // تقريب لرقمين عشريين
        return round(distance);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
