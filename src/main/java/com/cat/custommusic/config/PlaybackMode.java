package com.cat.custommusic.config;

import net.minecraft.network.chat.Component;

/**
 * Режими відтворення фонової музики.
 */
public enum PlaybackMode {
    RANDOM("gui.custommusic.mode.random", "Випадково"),
    SEQUENTIAL("gui.custommusic.mode.sequential", "По порядку");

    private final String translationKey;
    private final String displayName;

    PlaybackMode(String translationKey, String displayName) {
        this.translationKey = translationKey;
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Component getComponent() {
        return Component.translatable(translationKey);
    }

    public PlaybackMode next() {
        PlaybackMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
