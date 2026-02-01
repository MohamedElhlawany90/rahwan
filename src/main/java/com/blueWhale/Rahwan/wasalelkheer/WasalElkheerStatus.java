package com.blueWhale.Rahwan.wasalelkheer;

public enum WasalElkheerStatus {
    PENDING("Pending"),
    ACCEPTED("Accepted"),
    IN_PROGRESS("In Progress"),
    IN_THE_WAY("In The Way"),
    RETURN("Return"),
    DELIVERED("Delivered");

    private final String displayName;

    WasalElkheerStatus(String displayName) {
        this.displayName = displayName;
    }

}