package com.blueWhale.Rahwan.otp;

import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.util.Random;

@Service
public class OtpService {

    private final Random random = new SecureRandom();

    /**
     * توليد OTP من 6 أرقام
     */
    public String generateOtp() {
        return String.format("%06d", random.nextInt(1000000));
    }
}