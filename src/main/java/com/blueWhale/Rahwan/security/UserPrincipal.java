package com.blueWhale.Rahwan.security;

import com.blueWhale.Rahwan.user.User;
import com.blueWhale.Rahwan.user.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

@Data
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private UUID id;
    private String name;
    private String phone;
    private String password;
    private UserRole role;
    private boolean active;

    public static UserPrincipal create(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getName(),
                user.getPhone(),
                user.getPassword(),
                user.getRole() != null ? user.getRole() : UserRole.USER,
                user.isActive()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // إضافة الـ role كـ authority للـ Spring Security
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + role.name())
        );
    }

    @Override
    public String getUsername() {
        return phone;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    // Helper methods
    public boolean isUser() {
        return role == UserRole.USER;
    }

    public boolean isDriver() {
        return role == UserRole.DRIVER;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}