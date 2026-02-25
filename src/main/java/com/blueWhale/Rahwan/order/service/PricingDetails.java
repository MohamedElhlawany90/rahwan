package com.blueWhale.Rahwan.order.service;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingDetails {
    private double baseCost;
    private double costPerKm;
    private double roadMultiplier;
    private double distanceCost;
    private double totalCost;
    private String distanceDisplay;
    private double distanceKm;

    // ✨ حقول جديدة من Google Maps

    /**
     * الوقت المتوقع للرحلة بالدقائق (من Google Maps)
     */
    private Integer estimatedDuration;

    /**
     * مصدر حساب المسافة (GOOGLE_MAPS أو HAVERSINE_FALLBACK)
     */
    private String distanceSource;

    /**
     * ملاحظات إضافية عن الحساب
     */
    private String calculationNotes;

    /**
     * هل النتيجة من الـ cache
     */
    private boolean fromCache;

    /**
     * نص توضيحي للمدة (للعرض)
     */
    public String getDurationDisplay() {
        if (estimatedDuration == null) {
            return null;
        }
        if (estimatedDuration < 60) {
            return estimatedDuration + " دقيقة";
        }
        int hours = estimatedDuration / 60;
        int minutes = estimatedDuration % 60;
        return String.format("%d ساعة و %d دقيقة", hours, minutes);
    }
}