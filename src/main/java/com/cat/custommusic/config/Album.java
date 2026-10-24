package com.cat.custommusic.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Модель альбому (плейлиста) кастомної та ванільної музики.
 */
public class Album {
    private String id;
    private String name;
    private boolean permanent;
    private String sortMode = "custom"; // "name", "duration", "type", "custom"
    private List<String> trackIds = new ArrayList<>();

    public Album() {
    }

    public Album(String id, String name, boolean permanent, List<String> trackIds) {
        this.id = id;
        this.name = name;
        this.permanent = permanent;
        this.trackIds = trackIds != null ? new ArrayList<>(trackIds) : new ArrayList<>();
        this.sortMode = "custom";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isPermanent() {
        return permanent;
    }

    public void setPermanent(boolean permanent) {
        this.permanent = permanent;
    }

    public String getSortMode() {
        return sortMode != null ? sortMode : "custom";
    }

    public void setSortMode(String sortMode) {
        this.sortMode = sortMode;
    }

    public List<String> getTrackIds() {
        if (trackIds == null) {
            trackIds = new ArrayList<>();
        }
        return trackIds;
    }

    public void setTrackIds(List<String> trackIds) {
        this.trackIds = trackIds != null ? new ArrayList<>(trackIds) : new ArrayList<>();
    }
}
