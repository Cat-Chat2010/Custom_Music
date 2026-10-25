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

import com.cat.custommusic.mixin.SoundEngineAccessor;
import com.cat.custommusic.mixin.SoundManagerAccessor;
import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import java.util.List;
import java.util.Map;
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
    private TrackEntry lastPlayedTrack = null;
    private int nextSongDelayTicks = 100; // Початкова затримка 5 секунд при вході

    private int currentTrackElapsedTicks = 0;
    private int currentTrackTotalDurationTicks = 180 * 20;
    private boolean isManuallyStopped = false;
    private boolean isPaused = false;
    private boolean wasMuted = false;
    private int resumeCooldown = 0;
    private final java.util.ArrayDeque<TrackEntry> trackHistory = new java.util.ArrayDeque<>();
    private final java.util.concurrent.atomic.AtomicBoolean isSeeking = new java.util.concurrent.atomic.AtomicBoolean(false);
    private volatile int pendingSeekSeconds = -1;

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
        if (this.isManuallyStopped || this.isPaused) {
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

            // Якщо звук фізично активний — продовжуємо відлік часу
            if (active) {
                // Якщо минув повний час тривалості треку (+ 1 секунда запасу = 20 тіків) — завершуємо трек
                if (this.currentTrackElapsedTicks >= this.currentTrackTotalDurationTicks + 20) {
                    finishCurrentTrack();
                    return;
                }
                this.currentTrackElapsedTicks++;
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
        if (this.currentTrack != null) {
            this.lastPlayedTrack = this.currentTrack;
        }
        if (this.currentMusicInstance != null) {
            this.minecraft.getSoundManager().stop(this.currentMusicInstance);
        }
        this.currentMusicInstance = null;
        this.currentTrack = null;
        this.currentTrackElapsedTicks = 0;
        this.wasMuted = false;
        com.cat.custommusic.gui.NowPlayingHud.onTrackStopped();
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
            this.currentMusicInstance = new CustomMusicSoundInstance(soundEvent, this.currentTrack);
            this.minecraft.getSoundManager().play(this.currentMusicInstance);
            LOGGER.info("[CustomMusic] Відновлено звучання поточного треку: {}", this.currentTrack.getDisplayName());
        } catch (Exception e) {
            LOGGER.error("[CustomMusic] Помилка відновлення треку", e);
        }
    }

    /**
     * Обирає та запускає наступний трек по порядку з активного альбому (з циклічним переходом).
     */
    public void playNextTrack() {
        this.isManuallyStopped = false;
        MusicConfig config = CustomMusicMod.getInstance().getConfig();
        if (config == null) return;

        com.cat.custommusic.config.Album activeAlbum = config.getActiveAlbum();
        List<TrackEntry> allAlbumTracks = config.getTracksForAlbum(activeAlbum);
        if (allAlbumTracks.isEmpty()) {
            scheduleNextDelay();
            return;
        }

        TrackEntry reference = this.currentTrack != null ? this.currentTrack : this.lastPlayedTrack;
        int currentFullIndex = -1;
        if (reference != null) {
            for (int i = 0; i < allAlbumTracks.size(); i++) {
                if (allAlbumTracks.get(i).equals(reference)) {
                    currentFullIndex = i;
                    break;
                }
            }
        }

        // Шукаємо наступний увімкнений трек (циклічно вперед)
        TrackEntry nextTrack = null;
        for (int step = 1; step <= allAlbumTracks.size(); step++) {
            int candidateIndex = (currentFullIndex + step + allAlbumTracks.size()) % allAlbumTracks.size();
            TrackEntry candidate = allAlbumTracks.get(candidateIndex);
            if (candidate.isEnabled()) {
                nextTrack = candidate;
                break;
            }
        }

        if (nextTrack != null) {
            playTrack(nextTrack);
        } else {
            scheduleNextDelay();
        }
    }

    /**
     * Відтворює вказаний трек.
     */
    public void playTrack(TrackEntry track) {
        if (this.currentMusicInstance != null) {
            this.minecraft.getSoundManager().stop(this.currentMusicInstance);
            this.currentMusicInstance = null;
        }
        TrackEntry prev = this.currentTrack != null ? this.currentTrack : this.lastPlayedTrack;
        if (prev != null && !prev.equals(track)) {
            this.trackHistory.push(prev);
            if (this.trackHistory.size() > 50) {
                this.trackHistory.removeLast();
            }
        }
        this.isManuallyStopped = false;
        this.isPaused = false;
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
            this.currentMusicInstance = new CustomMusicSoundInstance(soundEvent, track);
            this.currentTrack = track;
            this.lastPlayedTrack = track;
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
        this.isPaused = false;
        if (this.currentMusicInstance != null) {
            this.minecraft.getSoundManager().stop(this.currentMusicInstance);
            this.currentMusicInstance = null;
            if (this.currentTrack != null) {
                this.lastPlayedTrack = this.currentTrack;
            }
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

    public boolean isPaused() {
        return this.isPaused;
    }

    @Nullable
    public TrackEntry getCurrentTrack() {
        return currentTrack;
    }

    public int getNextSongDelayTicks() {
        return nextSongDelayTicks;
    }

    /**
     * Повертає на попередній трек у поточному альбомі по порядку (з циклічним переходом).
     */
    public void playPreviousTrack() {
        this.isManuallyStopped = false;
        this.isPaused = false;
        MusicConfig config = CustomMusicMod.getInstance().getConfig();
        if (config == null) return;

        com.cat.custommusic.config.Album activeAlbum = config.getActiveAlbum();
        List<TrackEntry> allAlbumTracks = config.getTracksForAlbum(activeAlbum);
        if (allAlbumTracks.isEmpty()) return;

        TrackEntry reference = this.currentTrack != null ? this.currentTrack : this.lastPlayedTrack;
        int currentFullIndex = -1;
        if (reference != null) {
            for (int i = 0; i < allAlbumTracks.size(); i++) {
                if (allAlbumTracks.get(i).equals(reference)) {
                    currentFullIndex = i;
                    break;
                }
            }
        }

        // Шукаємо попередній увімкнений трек (циклічно назад)
        TrackEntry prevTrack = null;
        for (int step = 1; step <= allAlbumTracks.size(); step++) {
            int candidateIndex = (currentFullIndex - step + allAlbumTracks.size() * 2) % allAlbumTracks.size();
            TrackEntry candidate = allAlbumTracks.get(candidateIndex);
            if (candidate.isEnabled()) {
                prevTrack = candidate;
                break;
            }
        }

        if (prevTrack != null) {
            playTrack(prevTrack);
        }
    }

    public float getTrackEffectiveVolume(TrackEntry track) {
        if (track == null) return 1.0f;
        MusicConfig config = CustomMusicMod.getInstance().getConfig();
        if (config == null) return 1.0f;

        float typeFactor;
        if (track.isCustom()) {
            typeFactor = config.getCustomVolume();
        } else if (track.isDisc()) {
            typeFactor = config.getDiscVolume();
        } else {
            typeFactor = config.getVanillaVolume();
        }

        float trackFactor = config.getTrackVolume(track.getId());
        return Math.max(0.0f, Math.min(1.0f, typeFactor * trackFactor));
    }

    public void updateCurrentTrackVolume() {
        if (this.currentMusicInstance != null && this.currentTrack != null) {
            ChannelAccess.ChannelHandle handle = getChannelHandle(this.currentMusicInstance);
            if (handle != null) {
                float master = this.minecraft.options.getSoundSourceVolume(SoundSource.MASTER);
                float music = this.minecraft.options.getSoundSourceVolume(SoundSource.MUSIC);
                float eff = getTrackEffectiveVolume(this.currentTrack);
                float finalVol = master * music * eff;
                handle.execute(channel -> channel.setVolume(finalVol));
            }
        }
    }

    /**
     * Ставить відтворення на паузу через канал OpenAL.
     */
    public void pause() {
        if (this.currentMusicInstance != null && !this.isPaused) {
            ChannelAccess.ChannelHandle handle = getChannelHandle(this.currentMusicInstance);
            if (handle != null) {
                handle.execute(Channel::pause);
                this.isPaused = true;
                LOGGER.info("[CustomMusic] Відтворення призупинено.");
            }
        }
    }

    /**
     * Відновлює відтворення після паузи через канал OpenAL.
     */
    public void resume() {
        if (this.currentMusicInstance != null && this.isPaused) {
            ChannelAccess.ChannelHandle handle = getChannelHandle(this.currentMusicInstance);
            if (handle != null) {
                handle.execute(Channel::unpause);
                this.isPaused = false;
                LOGGER.info("[CustomMusic] Відтворення відновлено з паузи.");
            }
        }
    }

    /**
     * Перемикає стан: Грати / Пауза.
     */
    public void togglePlayPause() {
        if (this.currentMusicInstance != null) {
            if (this.isPaused) {
                resume();
            } else {
                pause();
            }
        } else {
            playNextTrack();
        }
    }

    /**
     * Перемотує поточний трек на вказану секунду з атомарним захистом від перевантаження.
     */
    public void seekTo(int targetSeconds) {
        if (this.currentMusicInstance == null || this.currentTrack == null) return;
        int total = getTrackTotalSeconds();
        int clamped = Math.max(0, Math.min(total - 1, targetSeconds));
        this.currentTrackElapsedTicks = clamped * 20;

        ChannelAccess.ChannelHandle handle = getChannelHandle(this.currentMusicInstance);
        if (handle == null) return;

        if (!isSeeking.compareAndSet(false, true)) {
            pendingSeekSeconds = clamped;
            return;
        }

        executeSeek(handle, clamped);
    }

    private void executeSeek(ChannelAccess.ChannelHandle handle, int targetSeconds) {
        final boolean wasPaused = this.isPaused;
        net.minecraft.Util.backgroundExecutor().execute(() -> {
            try {
                handle.execute(channel -> {
                    if (channel instanceof ISeekableChannel seekable) {
                        seekable.custommusic$seek(targetSeconds);
                        if (wasPaused) {
                            channel.pause();
                        }
                    }
                });
            } catch (Throwable t) {
                LOGGER.error("[CustomMusic] Помилка перемотки: ", t);
            } finally {
                isSeeking.set(false);
                int next = pendingSeekSeconds;
                if (next != -1) {
                    pendingSeekSeconds = -1;
                    if (next != targetSeconds) {
                        seekTo(next);
                    }
                }
            }
        });
    }

    /**
     * Перемотує поточний трек вперед (+) або назад (-) на вказану кількість секунд.
     */
    public void seekRelative(int deltaSeconds) {
        seekTo(getTrackElapsedSeconds() + deltaSeconds);
    }

    /**
     * Знаходить ChannelHandle для конкретного екземпляра SoundInstance в SoundEngine.
     */
    public ChannelAccess.ChannelHandle getChannelHandle(SoundInstance instance) {
        if (instance == null || this.minecraft == null) return null;
        try {
            SoundManager soundManager = this.minecraft.getSoundManager();
            SoundEngine soundEngine = ((SoundManagerAccessor) soundManager).custommusic$getSoundEngine();
            Map<SoundInstance, ChannelAccess.ChannelHandle> map = ((SoundEngineAccessor) soundEngine).custommusic$getInstanceToChannel();
            return map.get(instance);
        } catch (Throwable t) {
            LOGGER.warn("[CustomMusic] Не вдалося отримати ChannelHandle: {}", t.getMessage());
            return null;
        }
    }
}
