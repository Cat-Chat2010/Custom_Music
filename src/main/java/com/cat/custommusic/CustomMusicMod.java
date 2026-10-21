package com.cat.custommusic;

import com.cat.custommusic.audio.FfmpegConverter;
import com.cat.custommusic.audio.MusicDirectoryManager;
import com.cat.custommusic.config.MusicConfig;
import com.cat.custommusic.config.VanillaTracks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Головний клас клієнтського мода Custom Music для Minecraft 1.21.1 (NeoForge).
 */
@Mod(value = CustomMusicMod.MODID, dist = Dist.CLIENT)
public class CustomMusicMod {
    public static final String MODID = "custommusic";
    public static final Logger LOGGER = LoggerFactory.getLogger("CustomMusic");

    private static CustomMusicMod instance;

    private final MusicDirectoryManager directoryManager;
    private final FfmpegConverter ffmpegConverter;
    private MusicConfig config;

    public CustomMusicMod(IEventBus modEventBus) {
        instance = this;

        LOGGER.info("[CustomMusic] Ініціалізація мода Custom Music для Minecraft 1.21.1 NeoForge...");

        // 1. Ініціалізуємо менеджер директорій
        this.directoryManager = new MusicDirectoryManager();
        this.directoryManager.initDirectories();

        // 2. Ініціалізуємо FFmpeg конвертер
        this.ffmpegConverter = new FfmpegConverter(this.directoryManager);
        boolean ffmpegFound = this.ffmpegConverter.isAvailable();
        if (ffmpegFound) {
            LOGGER.info("[CustomMusic] FFmpeg готовий до роботи.");
        } else {
            LOGGER.warn("[CustomMusic] FFmpeg не знайдено. Конвертація файлів із папки import/ буде недоступна до встановлення FFmpeg.");
        }

        // 3. Завантажуємо конфігурацію settings.json
        this.config = MusicConfig.load(this.directoryManager.getSettingsFile());
        this.config.syncWithDisk(this.directoryManager.getTrackFiles(), VanillaTracks.getDefaultVanillaTracks());

        // 4. Підписуємось на події життєвого циклу
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(com.cat.custommusic.gui.CustomMusicKeyBindings::onRegisterKeyMappings);
        modEventBus.addListener((net.neoforged.neoforge.event.AddPackFindersEvent event) ->
                com.cat.custommusic.resource.DynamicMusicPackFinder.onAddPackFinders(event, this.directoryManager));
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("[CustomMusic] Клієнтське налаштування завершено. Перевірка папки import/ на наявність нових треків...");
            // Запускаємо первинну автоконвертацію у фоні при старті клієнта
            if (ffmpegConverter.isAvailable()) {
                ffmpegConverter.convertAllImportsAsync(msg -> LOGGER.info("[CustomMusic-Import] {}", msg))
                        .thenAccept(count -> {
                            if (count > 0) {
                                LOGGER.info("[CustomMusic] Автоконвертація завершена. Оновлюємо список треків...");
                                refreshTracks();
                            }
                        });
            }
        });
    }

    /**
     * Оновлює список треків, синхронізуючи конфігурацію з диском.
     */
    public void refreshTracks() {
        if (config != null && directoryManager != null) {
            config.syncWithDisk(directoryManager.getTrackFiles(), VanillaTracks.getDefaultVanillaTracks());
        }
    }

    public static CustomMusicMod getInstance() {
        return instance;
    }

    public MusicDirectoryManager getDirectoryManager() {
        return directoryManager;
    }

    public FfmpegConverter getFfmpegConverter() {
        return ffmpegConverter;
    }

    public MusicConfig getConfig() {
        return config;
    }
}
