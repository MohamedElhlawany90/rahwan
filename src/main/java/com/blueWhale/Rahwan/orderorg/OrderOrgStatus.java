package com.blueWhale.Rahwan.orderorg;

public enum OrderOrgStatus {
    PENDING("Pending"),
    ACCEPTED("Accepted"),
    COLLECTED("Collected"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String displayName;

    OrderOrgStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}