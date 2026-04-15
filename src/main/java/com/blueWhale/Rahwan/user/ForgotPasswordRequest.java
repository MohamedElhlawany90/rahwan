package com.blueWhale.Rahwan.user;

import lombok.Data;

@Data
public class ForgotPasswordRequest {
    private String phone;

    public String getPhone() {
        return phone;
    }
}