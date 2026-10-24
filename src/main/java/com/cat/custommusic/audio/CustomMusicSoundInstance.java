package com.cat.custommusic.audio;

import com.cat.custommusic.config.TrackEntry;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * Кастомний екземпляр звуку фонової музики, що динамічно застосовує множник гучності
 * для відповідного типу (кастомний/ванільний/платівка) та індивідуального треку.
 */
public class CustomMusicSoundInstance extends AbstractSoundInstance {
    private final TrackEntry track;

    public CustomMusicSoundInstance(SoundEvent soundEvent, TrackEntry track) {
        super(soundEvent, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
        this.track = track;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.looping = false;
        this.delay = 0;
        this.relative = true;
    }

    public TrackEntry getTrack() {
        return track;
    }

    @Override
    public float getVolume() {
        float eff = CustomMusicPlayer.getInstance().getTrackEffectiveVolume(this.track);
        return Math.max(0.0f, Math.min(1.0f, eff));
    }
}
