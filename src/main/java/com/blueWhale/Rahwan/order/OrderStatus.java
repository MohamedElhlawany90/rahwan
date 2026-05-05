package com.blueWhale.Rahwan.order;

public enum OrderStatus {
    PENDING,        // في انتظار التأكيد أو قبول سائق
    ACCEPTED,       // تم قبول الطلب من السائق
    IN_PROGRESS,    // جاري التنفيذ (السائق في الطريق للاستلام)
    IN_RETURN,         // فى الطريق للارجاع
    RETURNED,        // تم الارجاع
    DELIVERED,       // تم التسليم
    CANCELLED       // تم إلغاء الطلب
}