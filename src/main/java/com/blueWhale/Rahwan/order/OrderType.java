package com.blueWhale.Rahwan.order;

public enum OrderType {
    CLOTHES("Clothes"),
    ELECTRONICS("Electronics"),
    DOCUMENTS("Documents"),
    FOOD("Food"),
    FRAGILE("Fragile"),
    OTHER("Other");

    private final String displayName;

    OrderType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}