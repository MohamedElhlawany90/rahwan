package com.blueWhale.Rahwan.user;


/**
 * User Role Enum
 * Defines the three main roles in the system
 */
public enum UserRole {
    user,
    driver,
    admin;

    /**
     * Convert from String (for backward compatibility)
     */
    public static UserRole fromString(String role) {
        if (role == null) {
            return user; // default
        }

        switch (role.toLowerCase()) {
            case "user":
                return user;
            case "driver":
                return driver;
            case "admin":
                return admin;
            default:
                return user;
        }
    }
}