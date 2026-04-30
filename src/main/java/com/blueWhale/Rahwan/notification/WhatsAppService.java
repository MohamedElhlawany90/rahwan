package com.blueWhale.Rahwan.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * خدمة إرسال الرسائل عبر WhatsApp
 * مسؤولة عن كل أنواع الرسائل بما فيهم OTP
 */
@Service
@RequiredArgsConstructor
public class WhatsAppService {

    private final WhatsAppClient whatsAppClient;

    // ==================== OTP Messages ====================

    /**
     * 1️⃣ OTP للتحقق من الحساب (تسجيل جديد / تغيير كلمة المرور)
     * يُرسل للمستخدم عند التسجيل أو طلب تغيير كلمة المرور
     */
    public void sendUserVerificationOtp(String phone, String otp) {
        String message = """
                🔐 رمز التحقق من حسابك
                
                رمز التأكيد: %s
                
                ⚠️ لا تشارك هذا الرمز مع أي شخص.
                
                ---
                🔐 Your Account Verification OTP
                
                Code: %s
                
                ⚠️ Do not share this code with anyone.
                """.formatted(otp, otp);

        send(phone, message);
    }

    /**
     * 2️⃣ OTP الاستلام - يُرسل للمُرسِل (User)
     * عشان السائق ياخد الطلب منه عند الاستلام
     */
    public void sendPickupOtpToSender(String phone, String userName, String otp) {
        String message = """
                🔑 رمز الاستلام
                
                مرحباً %s،
                الرمز: %s
                
                📦 شارك هذا الرمز مع السائق عند وصوله لاستلام الطلب.
                
                ---
                🔑 Pickup OTP
                
                Hello %s,
                Code: %s
                
                📦 Share this code with the driver when they arrive for pickup.
                """.formatted(userName, otp, userName, otp);

        send(phone, message);
    }

    /**
     * 3️⃣ OTP التسليم - يُرسل للمُستلِم (Recipient)
     * عشان السائق يسلمله الطلب عند الوصول
     */
    public void sendDeliveryOtpToRecipient(String phone, String recipientName, String otp) {
        String message = """
                🔑 رمز التسليم
                
                مرحباً %s،
                الرمز: %s
                
                📦 شارك هذا الرمز مع السائق عند استلام الطلب.
                
                ---
                🔑 Delivery OTP
                
                Hello %s,
                Code: %s
                
                📦 Share this code with the driver upon delivery.
                """.formatted(recipientName, otp, recipientName, otp);

        send(phone, message);
    }

    // ==================== Order Notifications ====================

    public void sendReturnOrderOtp(String phone, String otp) {
        String message = """
                🔑 رمز تأكيد إرجاع الطلب
                
                الرمز: %s
                
                📦 شارك هذا الرمز مع السائق عند استلام الطلب لإرجاعه.
                سيتم خصم ضعف ثمن الشحن من محفظتك. 
                ---
                🔑 Return Order OTP
                
                Code: %s
                
                📦 Share this code with the driver when they come to pick up the return order.
                """.formatted(otp, otp);
        send(phone, message);

    }
    /**
     * تأكيد إنشاء الطلب للمستخدم
     */
    public void sendOrderConfirmation(String phone, String trackingNumber, double cost) {
        String message = """
                ✅ تم تأكيد الطلب!
                
                📦 رقم التتبع: %s
                💰 تكلفة التوصيل: %.2f جنيه
                
                شكراً لاستخدامك خدماتنا!
                
                ---
                ✅ Order Confirmed!
                
                📦 Tracking Number: %s
                💰 Delivery Cost: %.2f EGP
                
                Thank you for using our service!
                """.formatted(trackingNumber, cost, trackingNumber, cost);

        send(phone, message);
    }

    /**
     * تأكيد طلب وصل الخير (Donation)
     */
    public void sendWasalElkheerConfirmation(String phone, Long orderId) {
        String message = """
                ✅ تم تأكيد طلب التبرع!
                
                📦 رقم الطلب: %d
                🙏 شكراً لك على التبرع.
                
                سنخطرك عندما يقبل سائق الطلب.
                
                ---
                ✅ Donation Order Confirmed!
                
                📦 Order ID: %d
                🙏 Thank you for donating.
                
                We will notify you once a driver accepts the order.
                """.formatted(orderId, orderId);

        send(phone, message);
    }

    /**
     * إشعار للمستخدم بقبول السائق للطلب
     */
    public void sendDriverAcceptedNotification(String phone, String driverName, String trackingNumber) {
        String message = """
                🚗 قبل السائق طلبك!
                
                👤 السائق: %s
                📦 رقم التتبع: %s
                
                🔔 السائق في الطريق إليك، يُرجى الاستعداد.
                
                ---
                🚗 Driver Accepted Your Order!
                
                👤 Driver: %s
                📦 Tracking Number: %s
                
                🔔 The driver is on their way, please be ready.
                """.formatted(driverName, trackingNumber, driverName, trackingNumber);

        send(phone, message);
    }

    /**
     * تأكيد التسليم الناجح
     */
    public void sendDeliveryConfirmation(String phone, String trackingNumber) {
        String message = """
                ✅ تم تسليم الطلب بنجاح!
                
                📦 رقم التتبع: %s
                
                شكراً لاستخدامك خدماتنا!
                نتمنى خدمتك مرة أخرى. 🙏
                
                ---
                ✅ Order Delivered Successfully!
                
                📦 Tracking Number: %s
                
                Thank you for using our service!
                We hope to serve you again. 🙏
                """.formatted(trackingNumber, trackingNumber);

        send(phone, message);
    }

    /**
     * إشعار إلغاء الطلب
     */
    public void sendOrderCancellation(String phone, String trackingNumber, String reason) {
        String message = """
                ❌ تم إلغاء الطلب
                
                📦 رقم التتبع: %s
                📝 السبب: %s
                
                إذا كان لديك أي استفسار، يُرجى التواصل مع الدعم.
                
                ---
                ❌ Order Cancelled
                
                📦 Tracking Number: %s
                📝 Reason: %s
                
                If you have any questions, please contact support.
                """.formatted(trackingNumber, reason, trackingNumber, reason);

        send(phone, message);
    }

    // ==================== Core Send Method ====================

    /**
     * Method موحدة لإرسال أي رسالة عبر WhatsApp
     */
    public void send(String phone, String message) {
        WhatsAppMessageRequest request = WhatsAppMessageRequest.builder()
                .chatId(toChatId(phone))
                .text(message)
                .priority(0)
                .build();

        whatsAppClient.sendMessage(request);
    }

    /**
     * تحويل رقم الهاتف إلى chat_id بصيغة Wapilot
     */
    private String toChatId(String phone) {
        String normalized = phone.startsWith("+")
                ? phone.substring(1)
                : phone;

        if (normalized.startsWith("0")) {
            normalized = "2" + normalized;
        }

        return normalized + "@c.us";
    }
}