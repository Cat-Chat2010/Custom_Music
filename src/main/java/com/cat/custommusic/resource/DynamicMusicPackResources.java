package com.cat.custommusic.resource;

import com.cat.custommusic.CustomMusicMod;
import com.cat.custommusic.audio.MusicDirectoryManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Динамічний віртуальний ресурс-пак, що на льоту генерує sounds.json
 * та надає .ogg аудіопотоки напряму з папки custom_music/tracks/.
 * Забезпечує 100% валідні ідентифікатори ResourceLocation для будь-яких назв файлів.
 */
public class DynamicMusicPackResources implements PackResources {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicMusicPackResources.class);
    public static final String NAMESPACE = CustomMusicMod.MODID;

    private final PackLocationInfo locationInfo;
    private final MusicDirectoryManager directoryManager;

    public DynamicMusicPackResources(PackLocationInfo locationInfo, MusicDirectoryManager directoryManager) {
        this.locationInfo = locationInfo;
        this.directoryManager = directoryManager;
    }

    /**
     * Генерує безпечний, валідний для ResourceLocation ключ звуку
     * для будь-якого імені файлу (незалежно від пробілів, кирилиці чи спецсимволів).
     */
    public static String toSoundKey(String fileName) {
        String base = fileName.replaceAll("(?i)\\.(ogg|mp3)$", "").toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder("s_");
        for (char c : base.toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        sb.append('_').append(Math.abs(fileName.hashCode()));
        return sb.toString();
    }

    private Map<String, Path> buildKeyMap() {
        Map<String, Path> map = new HashMap<>();
        List<Path> trackFiles = directoryManager.getTrackFiles();
        for (Path p : trackFiles) {
            String key = toSoundKey(p.getFileName().toString());
            map.put(key, p);
        }
        return map;
    }

    @Override
    public PackLocationInfo location() {
        return locationInfo;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) {
        return null;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        if (type != PackType.CLIENT_RESOURCES || !NAMESPACE.equals(location.getNamespace())) {
            return null;
        }

        String path = location.getPath();

        // 1. Запит sounds.json
        if ("sounds.json".equals(path)) {
            byte[] jsonBytes = generateSoundsJson();
            return () -> new ByteArrayInputStream(jsonBytes);
        }

        // 2. Запит аудіофайлу sounds/<soundKey>.ogg
        if (path.startsWith("sounds/") && path.endsWith(".ogg")) {
            String key = path.substring("sounds/".length(), path.length() - ".ogg".length());
            Map<String, Path> keyMap = buildKeyMap();
            Path trackFile = keyMap.get(key);
            if (trackFile != null && Files.exists(trackFile)) {
                return () -> Files.newInputStream(trackFile);
            }

            // Прямий fallback за ім'ям файлу
            Path direct = directoryManager.getRootDir().resolve(path.substring("sounds/".length()));
            if (Files.exists(direct)) {
                return () -> Files.newInputStream(direct);
            }
        }

        return null;
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES || !NAMESPACE.equals(namespace)) {
            return;
        }

        if (path.isEmpty() || path.equals("sounds.json")) {
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(NAMESPACE, "sounds.json");
            output.accept(loc, () -> new ByteArrayInputStream(generateSoundsJson()));
        }

        if (path.isEmpty() || path.startsWith("sounds")) {
            Map<String, Path> keyMap = buildKeyMap();
            for (Map.Entry<String, Path> entry : keyMap.entrySet()) {
                ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(NAMESPACE, "sounds/" + entry.getKey() + ".ogg");
                output.accept(loc, () -> Files.newInputStream(entry.getValue()));
            }
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.CLIENT_RESOURCES ? Set.of(NAMESPACE) : Collections.emptySet();
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) {
        if ("pack".equals(deserializer.getMetadataSectionName())) {
            int packVersion = SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES);
            PackMetadataSection section = new PackMetadataSection(
                    Component.translatable("pack.custommusic.description"),
                    packVersion,
                    Optional.empty()
            );
            return (T) section;
        }
        return null;
    }

    /**
     * Генерує віртуальний sounds.json з гарантовано безпечними ResourceLocation ключами.
     */
    private byte[] generateSoundsJson() {
        JsonObject root = new JsonObject();
        Map<String, Path> keyMap = buildKeyMap();

        for (Map.Entry<String, Path> entry : keyMap.entrySet()) {
            String soundKey = entry.getKey();

            JsonObject soundObj = new JsonObject();
            soundObj.addProperty("category", "music");

            JsonArray soundsList = new JsonArray();
            JsonObject trackItem = new JsonObject();
            trackItem.addProperty("name", NAMESPACE + ":" + soundKey);
            trackItem.addProperty("stream", true); // Стрімінг аудіо
            soundsList.add(trackItem);

            soundObj.add("sounds", soundsList);
            root.add(soundKey, soundObj);
        }

        // 2. Реєструємо індивідуальні ванільні треки Minecraft для точного відтворення без рандому
        for (com.cat.custommusic.config.VanillaTracks.VanillaTrackInfo info : com.cat.custommusic.config.VanillaTracks.ALL_VANILLA_TRACKS) {
            JsonObject soundObj = new JsonObject();
            soundObj.addProperty("category", "music");

            JsonArray soundsList = new JsonArray();
            JsonObject trackItem = new JsonObject();
            trackItem.addProperty("name", info.soundPath());
            trackItem.addProperty("stream", true);
            soundsList.add(trackItem);

            soundObj.add("sounds", soundsList);
            root.add(info.key(), soundObj);
        }

        return root.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void close() {
    }
}
