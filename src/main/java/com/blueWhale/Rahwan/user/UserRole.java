package com.blueWhale.Rahwan.user;


/**
 * User Role Enum
 * Defines the three main roles in the system
 */
public enum UserRole {
    USER("User", "مستخدم"),
    DRIVER("Driver", "سائق"),
    ADMIN("Admin", "مدير");

    private final String displayNameEn;
    private final String displayNameAr;

    UserRole(String displayNameEn, String displayNameAr) {
        this.displayNameEn = displayNameEn;
        this.displayNameAr = displayNameAr;
    }

    public String getDisplayNameEn() {
        return displayNameEn;
    }

    public String getDisplayNameAr() {
        return displayNameAr;
    }

    /**
     * Convert from String (for backward compatibility)
     */
    public static UserRole fromString(String role) {
        if (role == null) {
            return USER; // default
        }

        switch (role.toLowerCase()) {
            case "user":
                return USER;
            case "driver":
                return DRIVER;
            case "admin":
                return ADMIN;
            default:
                return USER;
        }
    }
}