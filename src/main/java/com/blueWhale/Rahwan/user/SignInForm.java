package com.blueWhale.Rahwan.user;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignInForm {
    @NotBlank(message = "Username is required")
    private String phone;
    @NotBlank(message = "Password is required")
    private String password;
}
