package com.blueWhale.Rahwan.user;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignInDto {

    private UUID id;
    private String name;
    private String phone;
    private String profileImage;
    private boolean active;
    private Set<UserRole> roles;        // ✅ Set بدل role واحدة
    private boolean verifiedPhone;
    private LocalDateTime createdAt;

    private double walletBalance;
    private double frozenBalance;
    private double totalBalance;

    private String token;
}