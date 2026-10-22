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

    private int currentTrackElapsedTicks = 0;
    private int currentTrackTotalDurationTicks = 180 * 20;
    private boolean isManuallyStopped = false;
    private boolean wasMuted = false;
    private int resumeCooldown = 0;

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
        if (this.resumeCooldown > 0) {
            this.resumeCooldown--;
        }

        // Якщо користувач вручну зупинив музику — нові пісні автоматично НЕ починаються
        if (this.isManuallyStopped) {
            return;
        }

        float musicVolume = this.minecraft.options.getSoundSourceVolume(SoundSource.MUSIC);
        float masterVolume = this.minecraft.options.getSoundSourceVolume(SoundSource.MASTER);
        boolean isMuted = (musicVolume <= 0.001f || masterVolume <= 0.001f);

        // Перевіряємо, чи активне відтворення поточного треку
        if (this.currentMusicInstance != null) {
            boolean active = this.minecraft.getSoundManager().isActive(this.currentMusicInstance);

            // Якщо звук був вимкнений (mute), а тепер увімкнений назад (unmute)
            if (this.wasMuted && !isMuted) {
                this.wasMuted = false;
                if (!active && this.resumeCooldown <= 0) {
                    resumeCurrentTrack();
                    return;
                }
            }

            if (isMuted) {
                this.wasMuted = true;
                // Звук вимкнено — трек не перемикається!
                return;
            }

            // Якщо звук фізично активний — продовжуємо відлік часу (ніколи не перериваємо звук завчасно!)
            if (active) {
                this.currentTrackElapsedTicks++;
                if (this.currentTrackElapsedTicks > this.currentTrackTotalDurationTicks) {
                    this.currentTrackTotalDurationTicks = this.currentTrackElapsedTicks;
                }
                return;
            }

            // Перші 30 тіків даємо OpenAL на ініціалізацію каналу
            if (this.currentTrackElapsedTicks < 30) {
                this.currentTrackElapsedTicks++;
                return;
            }

            // Якщо трек більше не активний в OpenAL (закінчився файл) — завершуємо трек!
            finishCurrentTrack();
            return;
        }

        // Відлік тиші між треками
        if (this.nextSongDelayTicks > 0) {
            this.nextSongDelayTicks--;
            return;
        }

        // Затримка вичерпана, запускаємо наступний трек
        playNextTrack();
    }

    private void finishCurrentTrack() {
        LOGGER.info("[CustomMusic] Трек закінчився: {}", currentTrack != null ? currentTrack.getDisplayName() : "Unknown");
        if (this.currentMusicInstance != null) {
            this.minecraft.getSoundManager().stop(this.currentMusicInstance);
        }
        this.currentMusicInstance = null;
        this.currentTrack = null;
        this.currentTrackElapsedTicks = 0;
        this.wasMuted = false;
        scheduleNextDelay();
    }

    /**
     * Відновлює звучання поточного треку без зміни пісні (наприклад, після увімкнення звуку).
     */
    private void resumeCurrentTrack() {
        if (this.currentTrack == null) return;
        this.resumeCooldown = 20; // Захист від повторних викликів
        try {
            if (this.currentMusicInstance != null) {
                this.minecraft.getSoundManager().stop(this.currentMusicInstance);
            }
            ResourceLocation soundLocation;
            if (this.currentTrack.isCustom()) {
                String soundKey = com.cat.custommusic.resource.DynamicMusicPackResources.toSoundKey(this.currentTrack.getFileName());
                soundLocation = ResourceLocation.fromNamespaceAndPath(CustomMusicMod.MODID, soundKey);
            } else {
                soundLocation = ResourceLocation.parse(this.currentTrack.getId());
            }

            SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(soundLocation);
            this.currentMusicInstance = SimpleSoundInstance.forMusic(soundEvent);
            this.minecraft.getSoundManager().play(this.currentMusicInstance);
            LOGGER.info("[CustomMusic] Відновлено звучання поточного треку: {}", this.currentTrack.getDisplayName());
        } catch (Exception e) {
            LOGGER.error("[CustomMusic] Помилка відновлення треку", e);
        }
    }

    /**
     * Обирає та запускає наступний трек згідно з налаштуваннями.
     */
    public void playNextTrack() {
        this.isManuallyStopped = false;
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
        if (this.currentMusicInstance != null) {
            this.minecraft.getSoundManager().stop(this.currentMusicInstance);
            this.currentMusicInstance = null;
        }
        this.isManuallyStopped = false;
        this.wasMuted = false;
        this.resumeCooldown = 0;

        ResourceLocation soundLocation;
        if (track.isCustom()) {
            String soundKey = com.cat.custommusic.resource.DynamicMusicPackResources.toSoundKey(track.getFileName());
            soundLocation = ResourceLocation.fromNamespaceAndPath(CustomMusicMod.MODID, soundKey);
        } else {
            soundLocation = ResourceLocation.parse(track.getId());
        }

        try {
            SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(soundLocation);
            this.currentMusicInstance = SimpleSoundInstance.forMusic(soundEvent);
            this.currentTrack = track;
            this.currentTrackElapsedTicks = 0;

            // Запускаємо звук у SoundManager: він виконує resolve і обирає конкретний звуковий файл
            this.minecraft.getSoundManager().play(this.currentMusicInstance);

            String displayTitle = track.getDisplayName();
            if (track.isCustom()) {
                java.nio.file.Path tracksDir = CustomMusicMod.getInstance().getDirectoryManager().getTracksDir();
                String fn = track.getFileName() != null ? track.getFileName() : "";
                java.nio.file.Path oggPath = tracksDir.resolve(fn);
                if (!java.nio.file.Files.exists(oggPath) && !fn.toLowerCase(java.util.Locale.ROOT).endsWith(".ogg")) {
                    oggPath = tracksDir.resolve(fn + ".ogg");
                }
                int durSec = OggAudioHelper.getDurationInSeconds(oggPath);
                this.currentTrackTotalDurationTicks = durSec * 20;
            } else {
                int durSec = OggAudioHelper.getTrackDurationSeconds(track);
                this.currentTrackTotalDurationTicks = durSec * 20;
                String resolvedName = OggAudioHelper.getResolvedVanillaTrackName(this.currentMusicInstance);
                if (resolvedName != null && track.getId().startsWith("minecraft:")) {
                    displayTitle = resolvedName;
                }
            }

            com.cat.custommusic.gui.NowPlayingHud.onTrackChanged(displayTitle);
            LOGGER.info("[CustomMusic] Відтворення треку [{}]: {} (тривалість {}с)",
                    track.isCustom() ? "Custom" : "Vanilla", displayTitle, this.currentTrackTotalDurationTicks / 20);
        } catch (Exception e) {
            LOGGER.error("[CustomMusic] Помилка запуску треку: " + track.getDisplayName(), e);
            scheduleNextDelay();
        }
    }

    /**
     * Зупиняє поточне відтворення музики.
     */
    public void stopPlaying() {
        this.isManuallyStopped = true;
        if (this.currentMusicInstance != null) {
            this.minecraft.getSoundManager().stop(this.currentMusicInstance);
            this.currentMusicInstance = null;
            this.currentTrack = null;
            this.currentTrackElapsedTicks = 0;
            com.cat.custommusic.gui.NowPlayingHud.onTrackStopped();
        }
    }

    public boolean isManuallyStopped() {
        return this.isManuallyStopped;
    }

    public int getTrackElapsedSeconds() {
        return this.currentTrackElapsedTicks / 20;
    }

    public int getTrackTotalSeconds() {
        return Math.max(1, this.currentTrackTotalDurationTicks / 20);
    }

    public float getTrackProgress() {
        return Math.min(1.0f, (float) this.currentTrackElapsedTicks / (float) Math.max(1, this.currentTrackTotalDurationTicks));
    }

    public static String formatDuration(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    public String getFormattedElapsed() {
        return formatDuration(getTrackElapsedSeconds());
    }

    public String getFormattedRemaining() {
        int rem = Math.max(0, getTrackTotalSeconds() - getTrackElapsedSeconds());
        return "-" + formatDuration(rem);
    }

    public String getFormattedTotal() {
        return formatDuration(getTrackTotalSeconds());
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
        return this.currentMusicInstance != null;
    }

    @Nullable
    public TrackEntry getCurrentTrack() {
        return currentTrack;
    }

    public int getNextSongDelayTicks() {
        return nextSongDelayTicks;
    }
}
