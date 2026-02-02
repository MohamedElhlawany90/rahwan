
package com.blueWhale.Rahwan.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserForm {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^20\\d{10}$", message = "Phone must start with 20 and be 12 digits")
    private String phone;

    @NotBlank(message = "Type is required (user or driver or admin)")
    @Pattern(regexp = "^(user|driver|admin)$", message = "Type must be 'user' or 'driver'")
    private String type;
}