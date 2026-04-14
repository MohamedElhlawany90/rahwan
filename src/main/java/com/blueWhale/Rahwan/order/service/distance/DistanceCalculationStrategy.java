package com.blueWhale.Rahwan.order.service.distance;

import com.blueWhale.Rahwan.order.dto.DistanceResult;

/**
 * Strategy Interface لحساب المسافة
 */
public interface DistanceCalculationStrategy {

    /**
     * حساب المسافة بين نقطتين
     *
     * @param lat1 Latitude نقطة البداية
     * @param lon1 Longitude نقطة البداية
     * @param lat2 Latitude نقطة النهاية
     * @param lon2 Longitude نقطة النهاية
     * @return نتيجة الحساب مع تفاصيل المصدر
     */
    DistanceResult calculateDistance(double lat1, double lon1, double lat2, double lon2);

    /**
     * اسم الاستراتيجية
     */
    String getStrategyName();

    /**
     * هل الاستراتيجية متاحة للاستخدام
     */
    boolean isAvailable();
}