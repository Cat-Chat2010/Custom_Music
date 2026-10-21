package com.cat.custommusic.config;

import java.util.Objects;

/**
 * Запис одного треку в конфігурації.
 */
public class TrackEntry {
    private String id;
    private String displayName;
    private boolean custom;
    private boolean enabled;
    private String fileName;

    public TrackEntry() {
    }

    public TrackEntry(String id, String displayName, boolean custom, boolean enabled, String fileName) {
        this.id = id;
        this.displayName = displayName;
        this.custom = custom;
        this.enabled = enabled;
        this.fileName = fileName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isCustom() {
        return custom;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrackEntry that = (TrackEntry) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
