package com.cat.custommusic.audio;

import com.cat.custommusic.CustomMusicMod;
import com.cat.custommusic.config.MusicConfig;
import com.cat.custommusic.config.PlaybackMode;
import com.cat.custommusic.config.TrackEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Клієнтський менеджер черги та відтворення фонової музики.
 */
public class CustomMusicPlayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomMusicPlayer.class);
    private static CustomMusicPlayer instance;

    private final Minecraft minecraft;
    private final Random random = new Random();

    private SoundInstance currentMusicInstance = null;
    private TrackEntry currentTrack = null;
    private int nextSongDelayTicks = 100; // Початкова затримка 5 секунд при вході
    private int sequentialIndex = 0;

    public CustomMusicPlayer() {
        this.minecraft = Minecraft.getInstance();
    }

    public static synchronized CustomMusicPlayer getInstance() {
        if (instance == null) {
            instance = new CustomMusicPlayer();
        }
        return instance;
    }

    /**
     * Викликається кожен клієнтський тік замість ванільного MusicManager.tick().
     */
    public void tick() {
        // Перевіряємо, чи активне відтворення поточного треку
        if (this.currentMusicInstance != null) {
            if (this.minecraft.getSoundManager().isActive(this.currentMusicInstance)) {
                return;
            } else {
                // Трек завершився
                LOGGER.info("[CustomMusic] Трек закінчився: {}", currentTrack != null ? currentTrack.getDisplayName() : "Unknown");
                this.currentMusicInstance = null;
                this.currentTrack = null;
                scheduleNextDelay();
            }
        }

        // Відлік тиші між треками
        if (this.nextSongDelayTicks > 0) {
            this.nextSongDelayTicks--;
            return;
        }

        // Затримка вичерпана, запускаємо наступний трек
        playNextTrack();
    }

    /**
     * Обирає та запускає наступний трек згідно з налаштуваннями.
     */
    public void playNextTrack() {
        MusicConfig config = CustomMusicMod.getInstance().getConfig();
        if (config == null) return;

        List<TrackEntry> activeTracks = config.getTracks().stream()
                .filter(TrackEntry::isEnabled)
                .collect(Collectors.toList());

        if (activeTracks.isEmpty()) {
            // Немає активних треків — чекаємо
            scheduleNextDelay();
            return;
        }

        TrackEntry selectedTrack;
        if (config.getPlaybackMode() == PlaybackMode.RANDOM) {
            if (activeTracks.size() == 1) {
                selectedTrack = activeTracks.get(0);
            } else {
                // Обираємо випадковий трек, бажано не той самий щойно зіграний
                int index;
                do {
                    index = random.nextInt(activeTracks.size());
                    selectedTrack = activeTracks.get(index);
                } while (selectedTrack.equals(currentTrack) && activeTracks.size() > 1);
            }
        } else {
            // SEQUENTIAL: по порядку списку
            if (sequentialIndex >= activeTracks.size()) {
                sequentialIndex = 0;
            }
            selectedTrack = activeTracks.get(sequentialIndex);
            sequentialIndex = (sequentialIndex + 1) % activeTracks.size();
        }

        playTrack(selectedTrack);
    }

    /**
     * Відтворює вказаний трек.
     */
    public void playTrack(TrackEntry track) {
        stopPlaying();

        ResourceLocation soundLocation;
        if (track.isCustom()) {
            String soundName = track.getFileName().replace(".ogg", "");
            soundLocation = ResourceLocation.fromNamespaceAndPath(CustomMusicMod.MODID, soundName);
        } else {
            soundLocation = ResourceLocation.parse(track.getId());
        }

        try {
            SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(soundLocation);
            this.currentMusicInstance = SimpleSoundInstance.forMusic(soundEvent);
            this.currentTrack = track;
            this.minecraft.getSoundManager().play(this.currentMusicInstance);
            com.cat.custommusic.gui.NowPlayingHud.show(track.getDisplayName());
            LOGGER.info("[CustomMusic] Відтворення треку [{}]: {}", track.isCustom() ? "Custom" : "Vanilla", track.getDisplayName());
        } catch (Exception e) {
            LOGGER.error("[CustomMusic] Помилка запуску треку: " + track.getDisplayName(), e);
            scheduleNextDelay();
        }
    }

    /**
     * Зупиняє поточне відтворення музики.
     */
    public void stopPlaying() {
        if (this.currentMusicInstance != null) {
            this.minecraft.getSoundManager().stop(this.currentMusicInstance);
            this.currentMusicInstance = null;
            this.currentTrack = null;
        }
    }

    /**
     * Вираховує випадкову затримку тиші (у тіках) між треками.
     */
    public void scheduleNextDelay() {
        MusicConfig config = CustomMusicMod.getInstance().getConfig();
        if (config == null) {
            this.nextSongDelayTicks = 400;
            return;
        }

        int minSec = config.getMinDelaySeconds();
        int maxSec = config.getMaxDelaySeconds();
        int minTicks = minSec * 20;
        int maxTicks = maxSec * 20;

        if (maxTicks <= minTicks) {
            this.nextSongDelayTicks = minTicks;
        } else {
            this.nextSongDelayTicks = minTicks + random.nextInt(maxTicks - minTicks + 1);
        }

        LOGGER.info("[CustomMusic] Пауза до наступного треку: {} сек ({} тіків)", this.nextSongDelayTicks / 20, this.nextSongDelayTicks);
    }

    public boolean isPlaying() {
        return this.currentMusicInstance != null && this.minecraft.getSoundManager().isActive(this.currentMusicInstance);
    }

    @Nullable
    public TrackEntry getCurrentTrack() {
        return currentTrack;
    }

    public int getNextSongDelayTicks() {
        return nextSongDelayTicks;
    }
}
