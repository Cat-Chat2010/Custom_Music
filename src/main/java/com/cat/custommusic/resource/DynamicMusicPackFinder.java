package com.cat.custommusic.resource;

import com.cat.custommusic.CustomMusicMod;
import com.cat.custommusic.audio.MusicDirectoryManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Реєстратор динамічного ресурс-паку в системі ресурсів гри.
 */
public class DynamicMusicPackFinder implements RepositorySource {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicMusicPackFinder.class);
    public static final String PACK_ID = "custommusic_dynamic_resources";

    private final MusicDirectoryManager directoryManager;

    public DynamicMusicPackFinder(MusicDirectoryManager directoryManager) {
        this.directoryManager = directoryManager;
    }

    public static void onAddPackFinders(AddPackFindersEvent event, MusicDirectoryManager directoryManager) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            event.addRepositorySource(new DynamicMusicPackFinder(directoryManager));
            LOGGER.info("[CustomMusic] Зареєстровано динамічний ресурс-пак для аудіотреків.");
        }
    }

    @Override
    public void loadPacks(Consumer<Pack> packConsumer) {
        PackLocationInfo locationInfo = new PackLocationInfo(
                PACK_ID,
                Component.literal("Custom Music Audio Pack"),
                PackSource.BUILT_IN,
                Optional.empty()
        );

        Pack.ResourcesSupplier resourcesSupplier = new Pack.ResourcesSupplier() {
            @Override
            public net.minecraft.server.packs.PackResources openPrimary(PackLocationInfo info) {
                return new DynamicMusicPackResources(info, directoryManager);
            }

            @Override
            public net.minecraft.server.packs.PackResources openFull(PackLocationInfo info, Pack.Metadata metadata) {
                return new DynamicMusicPackResources(info, directoryManager);
            }
        };

        PackSelectionConfig selectionConfig = new PackSelectionConfig(
                true, // Обов'язковий (завжди активний)
                Pack.Position.TOP, // Найвищий пріоритет
                false // Фіксований
        );

        Pack pack = Pack.create(
                locationInfo,
                resourcesSupplier,
                PackType.CLIENT_RESOURCES,
                selectionConfig
        );

        if (pack != null) {
            packConsumer.accept(pack);
        }
    }
}
