package com.blueWhale.Rahwan.otp;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * خدمة OTP للطلبات (Orders)
 *
 * Single Responsibility: توليد OTP للطلبات فقط
 * ✅ لا تحتوي على logic الإرسال (DRY - تجنب التكرار)
 * ✅ WhatsAppService مسؤول عن الإرسال
 */
@Service
@RequiredArgsConstructor
public class OrderOtpService {

    private final OtpService otpService;

    /**
     * توليد OTP للاستلام (Pickup)
     * يستخدم OtpService.generateOtp() - DRY principle ✅
     */
    public String generatePickupOtp() {
        return otpService.generateOtp();
    }

    /**
     * توليد OTP للتسليم (Delivery)
     * يستخدم OtpService.generateOtp() - DRY principle ✅
     */
    public String generateDeliveryOtp() {
        return otpService.generateOtp();
    }
}