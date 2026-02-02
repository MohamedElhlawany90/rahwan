package com.blueWhale.Rahwan.user;

import lombok.Data;

@Data
public class ConfirmChangePasswordRequest {
    private String otp;
    private String newPassword;
}

