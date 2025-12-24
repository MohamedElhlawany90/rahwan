package com.blueWhale.Rahwan.otp;

import com.blueWhale.Rahwan.user.User;
import com.blueWhale.Rahwan.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserOtpService {

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final SmsService smsService;

    public String generateAndSendOtp(String phone) {
        String otp = otpService.generateOtp();

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("User not found with phone: " + phone));

        user.setOtpPhone(otp);
        user.setVerifiedPhone(false);
        userRepository.save(user);

        smsService.sendSms(phone, "Your verification OTP is: " + otp);

        return otp;
    }

    public boolean validateOtp(String phone, String otp) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("User not found with phone: " + phone));

        boolean isValid = otp.equals(user.getOtpPhone());

        if (isValid) {
            user.setVerifiedPhone(true);
        } else {
            user.setVerifiedPhone(false);
            user.setOtpPhone(null);
        }

        userRepository.save(user);
        return isValid;
    }
}