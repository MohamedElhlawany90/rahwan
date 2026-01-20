package com.blueWhale.Rahwan.wasalelkheer;

public enum WasalElkheerStatus {
    PENDING("Pending"),
    ACCEPTED("Accepted"),
    COLLECTED("Collected"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String displayName;

    WasalElkheerStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}