
package com.blueWhale.Rahwan.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserForm {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^20\\d{10}$", message = "Phone must start with 20 and be 12 digits")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    /**
     * Role: USER, DRIVER, or ADMIN
     * Default: USER
     */
    @NotNull(message = "Role is required")
    @Builder.Default
    private UserRole role = UserRole.USER;
}