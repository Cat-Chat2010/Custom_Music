package com.cat.custommusic.audio;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Менеджер робочої директорії для кастомної музики.
 * Всі файли (.mp3 та .ogg) зберігаються безпосередньо у папці custom_music/.
 */
public class MusicDirectoryManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MusicDirectoryManager.class);

    public static final String ROOT_FOLDER = "custom_music";
    public static final String SETTINGS_FILE = "custommusic.json";

    private final Path rootDir;
    private final Path settingsFile;

    public MusicDirectoryManager() {
        this.rootDir = FMLPaths.GAMEDIR.get().resolve(ROOT_FOLDER);
        this.settingsFile = this.rootDir.resolve(SETTINGS_FILE);
    }

    /**
     * Створює головну папку custom_music/ та автоматично переносить треки зі старих папок (якщо вони були).
     */
    public void initDirectories() {
        try {
            Files.createDirectories(rootDir);
            migrateOldDirectories();
            LOGGER.info("[CustomMusic] Робоча папка музики готова: {}", rootDir.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("[CustomMusic] Помилка створення робочої папки!", e);
        }
    }

    /**
     * Плавна міграція: переміщує треки зі старих підпапок tracks/ та import/ прямо в custom_music/.
     */
    public void migrateOldDirectories() {
        Path oldTracks = rootDir.resolve("tracks");
        if (Files.isDirectory(oldTracks)) {
            try (Stream<Path> stream = Files.list(oldTracks)) {
                stream.forEach(p -> {
                    if (Files.isRegularFile(p)) {
                        Path dest = rootDir.resolve(p.getFileName());
                        try {
                            Files.move(p, dest, StandardCopyOption.REPLACE_EXISTING);
                            LOGGER.info("[CustomMusic] Перенесено трек зі старої папки tracks/: {}", p.getFileName());
                        } catch (IOException e) {
                            LOGGER.error("[CustomMusic] Не вдалося перенести: " + p.getFileName(), e);
                        }
                    }
                });
            } catch (IOException ignored) {
            }
            try {
                Files.deleteIfExists(oldTracks);
            } catch (IOException ignored) {
            }
        }

        Path oldImport = rootDir.resolve("import");
        if (Files.isDirectory(oldImport)) {
            try (Stream<Path> stream = Files.list(oldImport)) {
                stream.forEach(p -> {
                    if (Files.isRegularFile(p)) {
                        String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
                        if (name.endsWith(".ogg") || name.endsWith(".mp3")) {
                            Path dest = rootDir.resolve(p.getFileName());
                            try {
                                Files.move(p, dest, StandardCopyOption.REPLACE_EXISTING);
                                LOGGER.info("[CustomMusic] Перенесено трек зі старої папки import/: {}", p.getFileName());
                            } catch (IOException e) {
                                LOGGER.error("[CustomMusic] Не вдалося перенести: " + p.getFileName(), e);
                            }
                        }
                    }
                });
            } catch (IOException ignored) {
            }
            try {
                Files.deleteIfExists(oldImport);
            } catch (IOException ignored) {
            }
        }

        Path oldFfmpeg = rootDir.resolve("ffmpeg");
        if (Files.isDirectory(oldFfmpeg)) {
            try {
                Files.deleteIfExists(oldFfmpeg.resolve("ffmpeg.exe"));
                Files.deleteIfExists(oldFfmpeg);
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Повертає список усіх готових до відтворення .ogg та .mp3 файлів з папки custom_music/.
     */
    public List<Path> getTrackFiles() {
        if (!Files.exists(rootDir)) {
            return Collections.emptyList();
        }
        try (Stream<Path> stream = Files.list(rootDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(this::isSupportedAudioFile)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("[CustomMusic] Не вдалося прочитати папку музики", e);
            return Collections.emptyList();
        }
    }

    public boolean isSupportedAudioFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".ogg") || name.endsWith(".mp3");
    }

    /**
     * Знаходить конкретний аудіофайл за ідентифікатором звуку ResourceLocation.
     */
    public Path findTrackFileByLocation(ResourceLocation location) {
        if (location == null || !"custommusic".equals(location.getNamespace())) {
            return null;
        }

        String path = location.getPath();
        String soundKey = path;
        if (path.startsWith("sounds/") && path.endsWith(".ogg")) {
            soundKey = path.substring("sounds/".length(), path.length() - ".ogg".length());
        }

        List<Path> files = getTrackFiles();
        for (Path f : files) {
            String key = com.cat.custommusic.resource.DynamicMusicPackResources.toSoundKey(f.getFileName().toString());
            if (key.equals(soundKey)) {
                return f;
            }
        }

        return null;
    }

    /**
     * Видаляє кастомний трек з папки custom_music/.
     */
    public boolean deleteTrackFile(String fileName) {
        Path target = rootDir.resolve(fileName);
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

    public Path getRootDir() {
        return rootDir;
    }

    public Path getTracksDir() {
        return rootDir;
    }

    public Path getSettingsFile() {
        return settingsFile;
    }
}
