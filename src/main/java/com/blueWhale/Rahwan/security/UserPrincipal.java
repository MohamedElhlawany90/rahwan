package com.blueWhale.Rahwan.security;

import com.blueWhale.Rahwan.user.User;
import com.blueWhale.Rahwan.user.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private UUID id;
    private String name;
    private String phone;
    private String password;
    private Set<UserRole> roles;   // ✅ Set بدل role واحدة
    private boolean active;

    public static UserPrincipal create(User user) {
        Set<UserRole> roles = user.getRoles() != null ? user.getRoles() : new HashSet<>();
        if (roles.isEmpty()) roles.add(UserRole.user); // fallback

        return new UserPrincipal(
                user.getId(),
                user.getName(),
                user.getPhone(),
                user.getPassword(),
                roles,
                user.isActive()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // ✅ كل الأدوار بتتحول لـ GrantedAuthority
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toList());
    }

    @Override
    public String getUsername() {
        return phone;
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return active; }

    // Helper methods - بيشوف لو الـ Set فيها الدور ده
    public boolean isUser()   { return roles != null && roles.contains(UserRole.user);   }
    public boolean isDriver() { return roles != null && roles.contains(UserRole.driver); }
    public boolean isAdmin()  { return roles != null && roles.contains(UserRole.admin);  }
}