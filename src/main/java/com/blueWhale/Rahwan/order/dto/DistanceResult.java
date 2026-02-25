package com.blueWhale.Rahwan.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * نتيجة موحدة لحساب المسافة من أي مصدر
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistanceResult {

    /**
     * المسافة بالكيلومتر
     */
    private double distanceKm;

    /**
     * المدة المتوقعة بالدقائق (من Google Maps فقط)
     */
    private Integer durationMinutes;

    /**
     * مصدر الحساب
     */
    private DistanceSource source;

    /**
     * هل النتيجة من الـ cache
     */
    private boolean fromCache;

    /**
     * رسالة إضافية (في حالة الـ fallback)
     */
    private String message;

    public enum DistanceSource {
        GOOGLE_MAPS,
        HAVERSINE_FALLBACK,
        CACHED
    }
}