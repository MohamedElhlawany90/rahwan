package com.blueWhale.Rahwan.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    private String oldPassword;
}

