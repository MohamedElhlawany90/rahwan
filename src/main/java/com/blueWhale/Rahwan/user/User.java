package com.blueWhale.Rahwan.user;

import com.blueWhale.Rahwan.wallet.Wallet;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(generator = "UUID")
    @org.hibernate.annotations.GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Wallet wallet;

    private String name;

    @Column(unique = true)
    private String phone;

    private String password;
    private String profileImage;

    @Column
    private String otpPhone;

    private String passwordResetOtp;
    private LocalDateTime passwordResetOtpExpiry;

    @Column(nullable = false)
    private boolean verifiedPhone = false;

    @Column(nullable = false)
    private boolean active = true;

    // user or driver
    @Column(nullable = false)
    private String type = "user";

    private LocalDateTime createdAt = LocalDateTime.now();
}