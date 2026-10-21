package com.cat.custommusic.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Керування конфігурацією мода (settings.json).
 */
public class MusicConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(MusicConfig.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private PlaybackMode playbackMode = PlaybackMode.RANDOM;
    private int minDelaySeconds = 20;
    private int maxDelaySeconds = 60;
    private List<TrackEntry> tracks = new ArrayList<>();

    // Не серіалізується у json
    private transient Path configPath;

    public MusicConfig() {
    }

    public static MusicConfig load(Path path) {
        MusicConfig config;
        if (Files.exists(path)) {
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                config = GSON.fromJson(reader, MusicConfig.class);
                if (config == null) {
                    config = new MusicConfig();
                }
            } catch (Exception e) {
                LOGGER.error("[CustomMusic] Помилка читання settings.json, створюємо новий за замовчуванням", e);
                config = new MusicConfig();
            }
        } else {
            config = new MusicConfig();
        }
        config.configPath = path;
        config.validateAndNormalize();
        config.save();
        return config;
    }

    public synchronized void save() {
        if (configPath == null) {
            return;
        }
        try {
            if (configPath.getParent() != null) {
                Files.createDirectories(configPath.getParent());
            }
            try (BufferedWriter writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            LOGGER.error("[CustomMusic] Не вдалося зберегти settings.json!", e);
        }
    }

    private void validateAndNormalize() {
        if (playbackMode == null) {
            playbackMode = PlaybackMode.RANDOM;
        }
        if (minDelaySeconds < 0) minDelaySeconds = 0;
        if (maxDelaySeconds < minDelaySeconds) maxDelaySeconds = minDelaySeconds;
        if (tracks == null) {
            tracks = new ArrayList<>();
        }
    }

    /**
     * Синхронізує список треків у конфігурації з файлами на диску та ванільними треками.
     */
    public synchronized void syncWithDisk(List<Path> diskOggFiles, List<TrackEntry> defaultVanillaTracks) {
        Map<String, TrackEntry> existingById = new LinkedHashMap<>();
        for (TrackEntry entry : tracks) {
            existingById.put(entry.getId(), entry);
        }

        List<TrackEntry> updatedTracks = new ArrayList<>();

        // 1. Спочатку зберігаємо вже існуючі треки, якщо вони ще існують на диску або є ванільними
        Set<String> presentOggNames = new HashSet<>();
        for (Path p : diskOggFiles) {
            presentOggNames.add(p.getFileName().toString());
        }

        for (TrackEntry existing : tracks) {
            if (existing.isCustom()) {
                if (presentOggNames.contains(existing.getFileName())) {
                    updatedTracks.add(existing);
                }
            } else {
                // Ванільний трек залишаємо
                updatedTracks.add(existing);
            }
        }

        // 2. Додаємо нові кастомні файли, яких ще не було в конфігу
        for (Path p : diskOggFiles) {
            String fileName = p.getFileName().toString();
            String id = "custommusic:" + fileName.replace(".ogg", "");
            if (!existingById.containsKey(id)) {
                String displayName = fileName.replace(".ogg", "").replace('_', ' ');
                TrackEntry newEntry = new TrackEntry(id, displayName, true, true, fileName);
                updatedTracks.add(newEntry);
                LOGGER.info("[CustomMusic] Додано новий кастомний трек до плейлиста: {}", fileName);
            }
        }

        // 3. Додаємо ванільні треки, якщо їх ще не було (наприклад при першому старті)
        if (defaultVanillaTracks != null) {
            for (TrackEntry vanilla : defaultVanillaTracks) {
                if (!existingById.containsKey(vanilla.getId())) {
                    updatedTracks.add(vanilla);
                }
            }
        }

        this.tracks = updatedTracks;
        save();
    }

    public PlaybackMode getPlaybackMode() {
        return playbackMode;
    }

    public void setPlaybackMode(PlaybackMode playbackMode) {
        this.playbackMode = playbackMode;
        save();
    }

    public int getMinDelaySeconds() {
        return minDelaySeconds;
    }

    public void setMinDelaySeconds(int minDelaySeconds) {
        this.minDelaySeconds = Math.max(0, minDelaySeconds);
        if (this.maxDelaySeconds < this.minDelaySeconds) {
            this.maxDelaySeconds = this.minDelaySeconds;
        }
        save();
    }

    public int getMaxDelaySeconds() {
        return maxDelaySeconds;
    }

    public void setMaxDelaySeconds(int maxDelaySeconds) {
        this.maxDelaySeconds = Math.max(this.minDelaySeconds, maxDelaySeconds);
        save();
    }

    public List<TrackEntry> getTracks() {
        return tracks;
    }

    public void moveTrack(int fromIndex, int toIndex) {
        if (fromIndex >= 0 && fromIndex < tracks.size() && toIndex >= 0 && toIndex < tracks.size()) {
            TrackEntry entry = tracks.remove(fromIndex);
            tracks.add(toIndex, entry);
            save();
        }
    }

    public void toggleTrack(TrackEntry entry) {
        if (entry != null) {
            entry.setEnabled(!entry.isEnabled());
            save();
        }
    }

    public void removeCustomTrack(TrackEntry entry) {
        if (entry != null && entry.isCustom()) {
            tracks.remove(entry);
            save();
        }
    }
}
