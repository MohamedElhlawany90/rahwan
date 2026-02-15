package com.blueWhale.Rahwan.otp;

import com.blueWhale.Rahwan.notification.WhatsAppService;
import com.blueWhale.Rahwan.user.User;
import com.blueWhale.Rahwan.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * خدمة OTP للمستخدمين (User Verification)
 * Single Responsibility: التحقق من هوية المستخدم عبر OTP
 * يعتمد على OtpService لتوليد OTP (DRY principle)
 */
@Service
@RequiredArgsConstructor
public class UserOtpService {

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final WhatsAppService whatsAppService;

    /**
     * توليد وإرسال OTP للتحقق من رقم الهاتف
     */
    public String generateAndSendOtp(String phone) {
        String otp = otpService.generateOtp();  // ✅ DRY

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("User not found with phone: " + phone));

        // حفظ OTP في الـ database
        user.setOtpPhone(otp);
        user.setVerifiedPhone(false);
        userRepository.save(user);

        // إرسال OTP عبر WhatsApp
        whatsAppService.sendOtp(phone, otp);

        return otp;
    }

    /**
     * التحقق من صحة OTP
     */
    public boolean validateOtp(String phone, String otp) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("User not found with phone: " + phone));

        boolean isValid = otp.equals(user.getOtpPhone());

        if (isValid) {
            user.setVerifiedPhone(true);
            user.setOtpPhone(null);  // مسح OTP بعد التحقق الناجح ✅
        }

        userRepository.save(user);
        return isValid;
    }

    /**
     * إعادة إرسال OTP
     * (للمستخدمين اللي ما وصلهمش الـ OTP)
     */
    public String resendOtp(String phone) {
        return generateAndSendOtp(phone);
    }
}