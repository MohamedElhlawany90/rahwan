package com.blueWhale.Rahwan.user;

import lombok.Data;

@Data
public class ForgotPasswordConfirmRequest {
    private String phone;
    private String otp;
    private String newPassword;

    public String getPhone() {
        return phone;
    }

    public String getOtp() {
        return otp;
    }

    public String getNewPassword() {
        return newPassword;
    }
}