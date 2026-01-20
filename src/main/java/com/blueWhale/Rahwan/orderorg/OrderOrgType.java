package com.blueWhale.Rahwan.orderorg;

public enum OrderOrgType {
    CLOTHES("Clothes"),
    FURNITURE("Furniture"),
    BOOKS("Books"),
    TOYS("Toys"),
    FOOD("Food"),
    ELECTRONICS("Electronics"),
    OTHER("Other");

    private final String displayName;

    OrderOrgType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}