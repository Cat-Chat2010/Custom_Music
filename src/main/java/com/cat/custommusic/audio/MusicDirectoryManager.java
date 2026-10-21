package com.cat.custommusic.audio;

import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Менеджер робочих директорій для кастомної музики.
 */
public class MusicDirectoryManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MusicDirectoryManager.class);

    public static final String ROOT_FOLDER = "custom_music";
    public static final String IMPORT_FOLDER = "import";
    public static final String TRACKS_FOLDER = "tracks";
    public static final String FFMPEG_FOLDER = "ffmpeg";
    public static final String SETTINGS_FILE = "settings.json";

    private static final Set<String> SUPPORTED_IMPORT_EXTENSIONS = Set.of(
            "mp3", "wav", "flac", "m4a", "aac", "wma", "opus", "ogg"
    );

    private final Path rootDir;
    private final Path importDir;
    private final Path tracksDir;
    private final Path ffmpegDir;
    private final Path settingsFile;

    public MusicDirectoryManager() {
        this.rootDir = FMLPaths.GAMEDIR.get().resolve(ROOT_FOLDER);
        this.importDir = this.rootDir.resolve(IMPORT_FOLDER);
        this.tracksDir = this.rootDir.resolve(TRACKS_FOLDER);
        this.ffmpegDir = this.rootDir.resolve(FFMPEG_FOLDER);
        this.settingsFile = this.rootDir.resolve(SETTINGS_FILE);
    }

    /**
     * Створює необхідну структуру папок при першому запуску.
     */
    public void initDirectories() {
        try {
            Files.createDirectories(rootDir);
            Files.createDirectories(importDir);
            Files.createDirectories(tracksDir);
            Files.createDirectories(ffmpegDir);
            LOGGER.info("[CustomMusic] Робочі директорії ініціалізовано: {}", rootDir.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("[CustomMusic] Помилка створення робочих директорій!", e);
        }
    }

    /**
     * Повертає список файлів з папки import/, які можна конвертувати.
     */
    public List<Path> getImportFiles() {
        if (!Files.exists(importDir)) {
            return Collections.emptyList();
        }
        try (Stream<Path> stream = Files.list(importDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(this::isSupportedAudioFile)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("[CustomMusic] Не вдалося прочитати папку import/", e);
            return Collections.emptyList();
        }
    }

    /**
     * Повертає список готових до відтворення .ogg файлів з папки tracks/.
     */
    public List<Path> getTrackFiles() {
        if (!Files.exists(tracksDir)) {
            return Collections.emptyList();
        }
        try (Stream<Path> stream = Files.list(tracksDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ogg"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("[CustomMusic] Не вдалося прочитати папку tracks/", e);
            return Collections.emptyList();
        }
    }

    /**
     * Видаляє кастомний трек з папки tracks/.
     */
    public boolean deleteTrackFile(String fileName) {
        Path target = tracksDir.resolve(fileName);
        if (!Files.exists(target)) {
            return false;
        }
        try {
            Files.delete(target);
            LOGGER.info("[CustomMusic] Видалено файл треку: {}", fileName);
            return true;
        } catch (IOException e) {
            LOGGER.error("[CustomMusic] Не вдалося видалити файл: {}", fileName, e);
            return false;
        }
    }

    public boolean isSupportedAudioFile(Path path) {
        String name = path.getFileName().toString();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == name.length() - 1) {
            return false;
        }
        String ext = name.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        return SUPPORTED_IMPORT_EXTENSIONS.contains(ext);
    }

    public Path getRootDir() {
        return rootDir;
    }

    public Path getImportDir() {
        return importDir;
    }

    public Path getTracksDir() {
        return tracksDir;
    }

    public Path getFfmpegDir() {
        return ffmpegDir;
    }

    public Path getSettingsFile() {
        return settingsFile;
    }
}
