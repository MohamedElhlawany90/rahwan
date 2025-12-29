package com.blueWhale.Rahwan.otp;

import com.blueWhale.Rahwan.notification.WhatsAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OrderOtpService {

    private final Random random = new SecureRandom();
    private final WhatsAppService whatsAppService;

    public String generatePickupOtp() {
        return String.format("%06d", random.nextInt(1000000));
    }

    public void sendPickupOtp(String phone, String message) {
        whatsAppService.send(phone, message);
    }

    public void sendDeliveryOtp(String phone, String otp) {
        String message = "🔑 Your Delivery OTP\n\n" +
                "Code: " + otp + "\n\n" +
                "Share this with the driver upon delivery.";
        whatsAppService.send(phone, message);
    }
}