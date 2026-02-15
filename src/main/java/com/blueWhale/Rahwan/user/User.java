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

    /**
     * User Role: USER, DRIVER, or ADMIN
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.USER;

    private LocalDateTime createdAt = LocalDateTime.now();

    // Helper methods for role checking
    public boolean isUser() {
        return this.role == UserRole.USER;
    }

    public boolean isDriver() {
        return this.role == UserRole.DRIVER;
    }

    public boolean isAdmin() {
        return this.role == UserRole.ADMIN;
    }
}