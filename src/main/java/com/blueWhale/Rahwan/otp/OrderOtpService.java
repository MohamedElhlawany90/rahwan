package com.blueWhale.Rahwan.otp;

import com.blueWhale.Rahwan.notification.WhatsAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * خدمة OTP للطلبات (Orders)
 * Single Responsibility: إرسال OTP للطلبات فقط
 * يعتمد على OtpService لتوليد OTP (DRY principle)
 */
@Service
@RequiredArgsConstructor
public class OrderOtpService {

    private final OtpService otpService;  // يستخدم OtpService بدل التكرار ✅
    private final WhatsAppService whatsAppService;

    /**
     * توليد OTP للاستلام
     * يستخدم OtpService.generateOtp() بدل التكرار
     */
    public String generatePickupOtp() {
        return otpService.generateOtp();  // ✅ DRY
    }

    /**
     * إرسال OTP الاستلام
     */
    public void sendPickupOtp(String phone, String message) {
        whatsAppService.send(phone, message);
    }

    /**
     * إرسال OTP التسليم
     */
    public void sendDeliveryOtp(String phone, String otp) {
        String message = """
                🔑 Your Delivery OTP
                
                Code: %s
                
                Share this with the driver upon delivery.
                """.formatted(otp);

        whatsAppService.send(phone, message);
    }

    /**
     * توليد وإرسال OTP الاستلام في خطوة واحدة
     * Helper method لتسهيل الاستخدام
     */
    public String generateAndSendPickupOtp(String phone, String userName) {
        String otp = generatePickupOtp();

        String message = """
                🔑 Your Pickup OTP
                
                Hello %s,
                Code: %s
                
                Share this code with the driver when they arrive for pickup.
                """.formatted(userName, otp);

        sendPickupOtp(phone, message);
        return otp;
    }

    /**
     * توليد وإرسال OTP التسليم في خطوة واحدة
     */
    public String generateAndSendDeliveryOtp(String phone, String recipientName) {
        String otp = otpService.generateOtp();

        String message = """
                🔑 Your Delivery OTP
                
                Hello %s,
                Code: %s
                
                Share this code with the driver upon delivery.
                """.formatted(recipientName, otp);

        whatsAppService.send(phone, message);
        return otp;
    }
}