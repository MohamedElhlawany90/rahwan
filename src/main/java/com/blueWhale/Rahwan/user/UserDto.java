// ============================================
// UserDto.java (Updated)
// ============================================
package com.blueWhale.Rahwan.user;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    private UUID id;
    private String name;
    private String phone;
    private String type;
    private String profileImage;
    private boolean active;
    private boolean verifiedPhone;
    private LocalDateTime createdAt;
    private double walletBalance;
    private String token ;
}