package com.blueWhale.Rahwan.wasalelkheer;

public enum WasalElkheerType {
    CLOTHES("Clothes"),
    FURNITURE("Furniture"),
    BOOKS("Books"),
    TOYS("Toys"),
    FOOD("Food"),
    ELECTRONICS("Electronics"),
    OTHER("Other");

    private final String displayName;

    WasalElkheerType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}