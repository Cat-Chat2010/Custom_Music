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
import net.minecraft.server.packs.resources.Resource;
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
 */
public class DynamicMusicPackResources implements PackResources {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicMusicPackResources.class);
    private static final String NAMESPACE = CustomMusicMod.MODID;

    private final PackLocationInfo locationInfo;
    private final MusicDirectoryManager directoryManager;

    public DynamicMusicPackResources(PackLocationInfo locationInfo, MusicDirectoryManager directoryManager) {
        this.locationInfo = locationInfo;
        this.directoryManager = directoryManager;
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

        // 2. Запит аудіофайлу sounds/<track_name>.ogg
        if (path.startsWith("sounds/") && path.endsWith(".ogg")) {
            String fileName = path.substring("sounds/".length());
            Path trackFile = directoryManager.getTracksDir().resolve(fileName);
            if (Files.exists(trackFile)) {
                return () -> Files.newInputStream(trackFile);
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
            List<Path> tracks = directoryManager.getTrackFiles();
            for (Path track : tracks) {
                String fileName = track.getFileName().toString();
                ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(NAMESPACE, "sounds/" + fileName);
                output.accept(loc, () -> Files.newInputStream(track));
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
                    Component.literal("Динамічний ресурс-пак для треків Custom Music"),
                    packVersion,
                    Optional.empty()
            );
            return (T) section;
        }
        return null;
    }

    /**
     * Генерує віртуальний sounds.json для всіх .ogg треків у папці tracks/.
     */
    private byte[] generateSoundsJson() {
        JsonObject root = new JsonObject();
        List<Path> trackFiles = directoryManager.getTrackFiles();

        for (Path trackFile : trackFiles) {
            String fileName = trackFile.getFileName().toString();
            String soundName = fileName.replace(".ogg", "");

            JsonObject soundObj = new JsonObject();
            soundObj.addProperty("category", "music");

            JsonArray soundsList = new JsonArray();
            JsonObject trackItem = new JsonObject();
            trackItem.addProperty("name", NAMESPACE + ":" + soundName);
            trackItem.addProperty("stream", true); // Стрімінг з диска без забивання оперативної пам'яті
            soundsList.add(trackItem);

            soundObj.add("sounds", soundsList);
            root.add(soundName, soundObj);
        }

        return root.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void close() {
        // Ресурсів відкритого дескриптора тримати не потрібно
    }
}
