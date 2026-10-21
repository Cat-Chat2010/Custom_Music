package com.cat.custommusic.config;

/**
 * Режими відтворення фонової музики.
 */
public enum PlaybackMode {
    RANDOM("Випадково"),
    SEQUENTIAL("По порядку");

    private final String displayName;

    PlaybackMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public PlaybackMode next() {
        PlaybackMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
