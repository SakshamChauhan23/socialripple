package com.social.ripple.external_ingestion.util.enumeration;

public enum Platform {
    X("X"),
    FACEBOOK("Facebook"),
    LINKEDIN("LinkedIn"),
    INSTAGRAM("Instagram");

    private final String displayName;

    Platform(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}