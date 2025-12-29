package com.blueWhale.Rahwan.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WhatsAppService {

    private final WhatsAppClient whatsAppClient;

    /**
     * إرسال OTP عبر WhatsApp
     */
    public void sendOtp(String phone, String otp) {
        String message = """
                🔐 Your OTP Code

                Code: %s

                ⚠️ Do not share this code with anyone.
                Valid for 10 minutes.
                """.formatted(otp);

        send(phone, message);
    }

    /**
     * إرسال تأكيد الطلب
     */
    public void sendOrderConfirmation(String phone, String trackingNumber, double cost) {
        String message = """
                ✅ Order Confirmed!

                📦 Tracking Number: %s
                💰 Delivery Cost: %.2f EGP

                Thank you for using our service!
                """.formatted(trackingNumber, cost);

        send(phone, message);
    }

    /**
     * إشعار قبول السائق
     */
    public void sendDriverAcceptedNotification(String phone, String driverName, String pickupOtp) {
        String message = """
                🚗 Driver Accepted Your Order!

                👤 Driver: %s
                🔑 Pickup OTP: %s

                Please be ready for pickup.
                """.formatted(driverName, pickupOtp);

        send(phone, message);
    }

    /**
     * إرسال OTP التسليم للمستلم
     */
    public void sendDeliveryOtpToRecipient(String phone, String recipientName, String otp) {
        String message = """
                📦 Delivery OTP

                👤 Recipient: %s
                🔑 OTP: %s

                Share this code with the driver upon delivery.
                """.formatted(recipientName, otp);

        send(phone, message);
    }

    /**
     * إرسال تأكيد التسليم
     */
    public void sendDeliveryConfirmation(String phone, String trackingNumber) {
        String message = """
                ✅ Order Delivered Successfully!

                📦 Tracking Number: %s

                Thank you for using our service!
                We hope to serve you again. 🙏
                """.formatted(trackingNumber);

        send(phone, message);
    }

    /**
     * ميثود موحدة للإرسال
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
     * تحويل phone → chat_id (Wapilot format)
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
