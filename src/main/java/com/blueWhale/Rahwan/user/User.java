package com.blueWhale.Rahwan.user;

import com.blueWhale.Rahwan.wallet.Wallet;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
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
     * User Roles: مجموعة من الأدوار - يوزر ممكن يكون user وdriver في نفس الوقت
     * Default: {user}
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Set<UserRole> roles = new HashSet<>();

    private LocalDateTime createdAt = LocalDateTime.now();

    // Helper methods for role checking
    public boolean isUser() {
        return roles != null && roles.contains(UserRole.user);
    }

    public boolean isDriver() {
        return roles != null && roles.contains(UserRole.driver);
    }

    public boolean isAdmin() {
        return roles != null && roles.contains(UserRole.admin);
    }

    /**
     * للتوافق مع الكود القديم - يرجع أعلى رول
     */
    public UserRole getPrimaryRole() {
        if (isAdmin()) return UserRole.admin;
        if (isDriver() && isUser()) return UserRole.driver; // لو الاتنين، driver أولوية
        if (isDriver()) return UserRole.driver;
        return UserRole.user;
    }
}