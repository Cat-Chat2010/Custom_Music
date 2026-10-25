package com.cat.custommusic.config;

import com.cat.custommusic.audio.OggAudioHelper;
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
 * Керування конфігурацією мода (settings.json):
 * - Альбоми та плейлисти.
 * - Регулювання гучності за категоріями (кастомна, ванільна, платівки) та для окремих треків.
 * - Налаштування затримок між треками.
 */
public class MusicConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(MusicConfig.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private PlaybackMode playbackMode = PlaybackMode.SEQUENTIAL;
    private int minDelaySeconds = 20;
    private int maxDelaySeconds = 60;
    private List<TrackEntry> tracks = new ArrayList<>();

    // Налаштування гучності (0.0f - 1.0f)
    private float customVolume = 1.0f;
    private float vanillaVolume = 1.0f;
    private float discVolume = 1.0f;
    private Map<String, Float> trackVolumes = new HashMap<>();

    // Альбоми
    private List<Album> albums = new ArrayList<>();
    private String activeAlbumId = "all_tracks";

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
            playbackMode = PlaybackMode.SEQUENTIAL;
        }
        if (minDelaySeconds < 0) minDelaySeconds = 0;
        if (maxDelaySeconds < minDelaySeconds) maxDelaySeconds = minDelaySeconds;
        if (tracks == null) {
            tracks = new ArrayList<>();
        }
        if (trackVolumes == null) {
            trackVolumes = new HashMap<>();
        } else {
            // Видаляємо пісні зі 100% гучністю
            trackVolumes.entrySet().removeIf(entry -> entry.getValue() == null || Math.abs(entry.getValue() - 1.0f) < 0.005f);
        }

        customVolume = Math.max(0.0f, Math.min(1.0f, customVolume));
        vanillaVolume = Math.max(0.0f, Math.min(1.0f, vanillaVolume));
        discVolume = Math.max(0.0f, Math.min(1.0f, discVolume));

        if (albums == null) {
            albums = new ArrayList<>();
        }

        // Перевіряємо наявність обов'язкового перманентного альбому "Усі пісні"
        Album allTracksAlbum = null;
        for (Album a : albums) {
            if ("all_tracks".equals(a.getId()) || a.isPermanent()) {
                allTracksAlbum = a;
                break;
            }
        }

        if (allTracksAlbum == null) {
            allTracksAlbum = new Album("all_tracks", "Усі пісні", true, new ArrayList<>());
            albums.add(0, allTracksAlbum);
        } else {
            allTracksAlbum.setId("all_tracks");
            allTracksAlbum.setPermanent(true);
        }

        // Синхронізуємо треки альбому "all_tracks"
        List<String> allIds = new ArrayList<>();
        Set<String> existingInAll = new LinkedHashSet<>(allTracksAlbum.getTrackIds());
        for (String id : existingInAll) {
            if (hasTrackWithId(id)) {
                allIds.add(id);
            }
        }
        for (TrackEntry t : tracks) {
            if (!existingInAll.contains(t.getId())) {
                allIds.add(t.getId());
            }
        }
        allTracksAlbum.setTrackIds(allIds);

        // Перевіряємо валідність активного альбому
        if (activeAlbumId == null || !hasAlbumWithId(activeAlbumId)) {
            activeAlbumId = "all_tracks";
        }

        // Міграція старих назв альбомів ("Альбом N" / "Album N") на "1", "2", ...
        for (Album a : albums) {
            if (!a.isPermanent() && a.getName() != null) {
                String trimmed = a.getName().trim();
                if (trimmed.matches("(?i)^(Альбом|Album)\\s*\\d+$")) {
                    int num = 1;
                    while (hasAlbumWithName(String.valueOf(num))) {
                        num++;
                    }
                    a.setName(String.valueOf(num));
                }
            }
        }
    }

    private boolean hasTrackWithId(String id) {
        if (id == null) return false;
        for (TrackEntry t : tracks) {
            if (id.equals(t.getId())) return true;
        }
        return false;
    }

    private boolean hasAlbumWithId(String id) {
        if (id == null) return false;
        for (Album a : albums) {
            if (id.equals(a.getId())) return true;
        }
        return false;
    }

    private boolean hasAlbumWithName(String name) {
        if (name == null) return false;
        for (Album a : albums) {
            if (name.equalsIgnoreCase(a.getName())) return true;
        }
        return false;
    }

    /**
     * Синхронізує список треків у конфігурації з файлами на диску та ванільними треками.
     */
    public synchronized void syncWithDisk(List<Path> diskAudioFiles, List<TrackEntry> defaultVanillaTracks) {
        Map<String, TrackEntry> existingById = new LinkedHashMap<>();
        for (TrackEntry entry : tracks) {
            existingById.put(entry.getId(), entry);
        }

        List<TrackEntry> updatedTracks = new ArrayList<>();
        Set<String> updatedIds = new HashSet<>();

        // 1. Спочатку зберігаємо вже існуючі треки, якщо вони ще існують на диску або є валідними ванільними треками
        Set<String> presentAudioNames = new HashSet<>();
        for (Path p : diskAudioFiles) {
            presentAudioNames.add(p.getFileName().toString());
        }

        for (TrackEntry existing : tracks) {
            if (existing.isCustom()) {
                if (presentAudioNames.contains(existing.getFileName())) {
                    updatedTracks.add(existing);
                    updatedIds.add(existing.getId());
                }
            } else {
                String id = existing.getId();
                if (id != null && id.contains("echo_in_the_canyon")) {
                    id = id.replace("echo_in_the_canyon", "echo_in_the_wind");
                    existing = new TrackEntry(id, "Aaron Cherof - Echo in the Wind", false, existing.isEnabled(), null);
                }
                VanillaTracks.VanillaTrackInfo info = VanillaTracks.findByKey(id);
                if (info != null && !updatedIds.contains(existing.getId())) {
                    existing.setDisplayName(info.displayName());
                    updatedTracks.add(existing);
                    updatedIds.add(existing.getId());
                }
            }
        }

        // 2. Додаємо нові кастомні файли, яких ще не було в конфігу
        for (Path p : diskAudioFiles) {
            String fileName = p.getFileName().toString();
            String id = "custommusic:" + com.cat.custommusic.resource.DynamicMusicPackResources.toSoundKey(fileName);
            if (!updatedIds.contains(id)) {
                String baseName = fileName.replaceAll("(?i)\\.(ogg|mp3)$", "");
                String displayName = baseName.replace('_', ' ');
                TrackEntry newEntry = new TrackEntry(id, displayName, true, true, fileName);
                updatedTracks.add(newEntry);
                updatedIds.add(id);
                LOGGER.info("[CustomMusic] Додано новий кастомний трек до плейлиста: {}", fileName);
            }
        }

        // 3. Додаємо ванільні треки, якщо їх ще не було
        if (defaultVanillaTracks != null) {
            for (TrackEntry vanilla : defaultVanillaTracks) {
                if (!updatedIds.contains(vanilla.getId())) {
                    updatedTracks.add(vanilla);
                    updatedIds.add(vanilla.getId());
                }
            }
        }

        this.tracks = updatedTracks;
        validateAndNormalize();
        save();
    }

    public List<TrackEntry> getTracks() {
        return tracks;
    }

    public TrackEntry getTrackById(String id) {
        if (id == null) return null;
        for (TrackEntry t : tracks) {
            if (id.equals(t.getId())) return t;
        }
        return null;
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

    public void toggleTrack(TrackEntry entry) {
        if (entry != null) {
            entry.setEnabled(!entry.isEnabled());
            save();
        }
    }

    /**
     * Повне видалення кастомного треку звідусіль (із загального списку, дисків, альбомів та налаштувань гучності).
     */
    public synchronized void removeCustomTrack(TrackEntry entry) {
        if (entry != null && entry.isCustom()) {
            tracks.remove(entry);
            trackVolumes.remove(entry.getId());
            for (Album a : albums) {
                a.getTrackIds().remove(entry.getId());
            }
            save();
        }
    }

    // ==================== АЛЬБОМИ ====================

    public List<Album> getAlbums() {
        return albums;
    }

    public Album getActiveAlbum() {
        for (Album a : albums) {
            if (a.getId().equals(activeAlbumId)) {
                return a;
            }
        }
        if (!albums.isEmpty()) {
            activeAlbumId = albums.get(0).getId();
            return albums.get(0);
        }
        Album fallback = new Album("all_tracks", "Усі пісні", true, new ArrayList<>());
        albums.add(fallback);
        activeAlbumId = fallback.getId();
        return fallback;
    }

    public void setActiveAlbum(Album album) {
        if (album != null) {
            this.activeAlbumId = album.getId();
            save();
        }
    }

    /**
     * Створює новий альбом. Якщо ім'я порожнє, автоматично присвоює "1", "2" і т.д. без слова "Альбом".
     */
    public synchronized Album createAlbum(String name) {
        if (name == null || name.trim().isEmpty()) {
            int num = 1;
            while (hasAlbumWithName(String.valueOf(num))) {
                num++;
            }
            name = String.valueOf(num);
        }
        String id = "album_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 5);
        Album album = new Album(id, name.trim(), false, new ArrayList<>());
        this.albums.add(album);
        this.activeAlbumId = id;
        save();
        return album;
    }

    /**
     * Видаляє альбом (не дозволяє видаляти перманентний альбом "Усі пісні").
     */
    public synchronized boolean deleteAlbum(String albumId) {
        Album toRemove = null;
        for (Album a : albums) {
            if (a.getId().equals(albumId)) {
                if (a.isPermanent()) return false;
                toRemove = a;
                break;
            }
        }
        if (toRemove != null) {
            albums.remove(toRemove);
            if (activeAlbumId.equals(albumId)) {
                activeAlbumId = "all_tracks";
            }
            save();
            return true;
        }
        return false;
    }

    /**
     * Повертає впорядкований список TrackEntry для конкретного альбому.
     */
    public List<TrackEntry> getTracksForAlbum(Album album) {
        if (album == null) return Collections.emptyList();
        List<TrackEntry> result = new ArrayList<>();
        if (album.isPermanent()) {
            Set<String> added = new HashSet<>();
            for (String id : album.getTrackIds()) {
                TrackEntry t = getTrackById(id);
                if (t != null && !added.contains(id)) {
                    result.add(t);
                    added.add(id);
                }
            }
            // Додаємо нові треки, яких ще не було в ID
            for (TrackEntry t : tracks) {
                if (!added.contains(t.getId())) {
                    result.add(t);
                    album.getTrackIds().add(t.getId());
                }
            }
        } else {
            List<String> valid = new ArrayList<>();
            for (String id : album.getTrackIds()) {
                TrackEntry t = getTrackById(id);
                if (t != null) {
                    result.add(t);
                    valid.add(id);
                }
            }
            album.setTrackIds(valid);
        }
        return result;
    }

    public synchronized void addTrackToAlbum(Album album, String trackId) {
        if (album == null || album.isPermanent() || trackId == null) return;
        if (!album.getTrackIds().contains(trackId)) {
            album.getTrackIds().add(trackId);
            save();
        }
    }

    public synchronized void removeTrackFromAlbum(Album album, String trackId) {
        if (album == null || album.isPermanent() || trackId == null) return;
        if (album.getTrackIds().remove(trackId)) {
            save();
        }
    }

    public synchronized void moveAlbumTrack(Album album, int fromIndex, int toIndex) {
        if (album == null) return;
        List<String> ids = album.getTrackIds();
        if (fromIndex >= 0 && fromIndex < ids.size() && toIndex >= 0 && toIndex < ids.size()) {
            String item = ids.remove(fromIndex);
            ids.add(toIndex, item);
            album.setSortMode("custom"); // Автоматично вмикає режим "Своє"
            save();
        }
    }

    public synchronized void sortAlbum(Album album, String sortMode) {
        if (album == null) return;
        album.setSortMode(sortMode);
        if ("custom".equals(sortMode)) {
            save();
            return;
        }

        List<TrackEntry> albumTracks = getTracksForAlbum(album);
        if ("name".equals(sortMode)) {
            albumTracks.sort(Comparator.comparing(t -> t.getDisplayName().toLowerCase(Locale.ROOT)));
        } else if ("duration".equals(sortMode)) {
            albumTracks.sort(Comparator.comparingInt(OggAudioHelper::getTrackDurationSeconds));
        } else if ("type".equals(sortMode)) {
            albumTracks.sort((a, b) -> {
                int typeA = a.isCustom() ? 0 : (a.isDisc() ? 2 : 1);
                int typeB = b.isCustom() ? 0 : (b.isDisc() ? 2 : 1);
                if (typeA != typeB) return Integer.compare(typeA, typeB);
                return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
            });
        }

        List<String> newIds = new ArrayList<>();
        for (TrackEntry t : albumTracks) {
            newIds.add(t.getId());
        }
        album.setTrackIds(newIds);
        save();
    }

    public synchronized void shuffleAlbum(Album album) {
        if (album == null) return;
        Collections.shuffle(album.getTrackIds());
        album.setSortMode("custom");
        save();
    }

    // ==================== ГУЧНІСТЬ ====================

    public float getCustomVolume() {
        return customVolume;
    }

    public void setCustomVolume(float customVolume) {
        this.customVolume = Math.max(0.0f, Math.min(1.0f, customVolume));
        save();
    }

    public float getVanillaVolume() {
        return vanillaVolume;
    }

    public void setVanillaVolume(float vanillaVolume) {
        this.vanillaVolume = Math.max(0.0f, Math.min(1.0f, vanillaVolume));
        save();
    }

    public float getDiscVolume() {
        return discVolume;
    }

    public void setDiscVolume(float discVolume) {
        this.discVolume = Math.max(0.0f, Math.min(1.0f, discVolume));
        save();
    }

    public float getTrackVolume(String trackId) {
        if (trackId == null || trackVolumes == null) return 1.0f;
        return trackVolumes.getOrDefault(trackId, 1.0f);
    }

    public synchronized void setTrackVolume(String trackId, float volume) {
        if (trackId == null) return;
        if (trackVolumes == null) {
            trackVolumes = new HashMap<>();
        }
        float clamped = Math.max(0.0f, Math.min(1.0f, volume));
        if (Math.abs(clamped - 1.0f) < 0.005f) {
            // При досягненні 100% пісня видаляється з налаштувань
            trackVolumes.remove(trackId);
        } else {
            trackVolumes.put(trackId, clamped);
        }
        save();
    }

    public Map<String, Float> getTrackVolumes() {
        if (trackVolumes == null) {
            trackVolumes = new HashMap<>();
        }
        return trackVolumes;
    }
}
