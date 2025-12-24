package com.blueWhale.Rahwan.otp;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderOtpService {

    private final OtpService otpService;
    private final SmsService smsService;

    public String generatePickupOtp() {
        return otpService.generateOtp();
    }

    public void sendPickupOtp(String phone, String otp) {
        smsService.sendSms(phone, "Your pickup OTP is: " + otp);
    }

    public void sendDeliveryOtp(String phone, String otp) {
        smsService.sendSms(phone, "Your delivery OTP is: " + otp);
    }

    public void sendOrderAcceptedNotification(String phone, String message) {
        smsService.sendSms(phone, message);
    }
}