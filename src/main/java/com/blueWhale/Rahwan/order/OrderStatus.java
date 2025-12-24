package com.blueWhale.Rahwan.order;

public enum OrderStatus {
    PENDING,        // في انتظار التأكيد أو قبول سائق
    ACCEPTED,       // تم قبول الطلب من السائق
    IN_PROGRESS,    // جاري التنفيذ (السائق في الطريق للاستلام)
    IN_THE_WAY,     // في الطريق للتسليم
    RETURN,         // تم الإرجاع
    DELIVERED       // تم التسليم
}