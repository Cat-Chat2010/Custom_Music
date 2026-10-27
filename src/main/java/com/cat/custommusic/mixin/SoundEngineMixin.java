package com.cat.custommusic.mixin;

import com.cat.custommusic.audio.CustomMusicPlayer;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEngineExecutor;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Mixin для SoundEngine, що забезпечує збереження відтворення музики при зміні світів/вимірів.
 */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @Shadow private boolean loaded;
    @Shadow private int tickCount;
    @Shadow @Final private SoundEngineExecutor executor;
    @Shadow @Final private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;
    @Shadow @Final private Map<SoundInstance, Integer> queuedSounds;
    @Shadow @Final private List<TickableSoundInstance> tickingSounds;
    @Shadow @Final private Multimap<SoundSource, SoundInstance> instanceBySource;
    @Shadow @Final private Map<SoundInstance, Integer> soundDeleteTime;
    @Shadow @Final private List<TickableSoundInstance> queuedTickableSounds;

    @Unique
    private boolean custommusic$isDestroying = false;

    @Inject(method = "destroy", at = @At("HEAD"))
    private void onDestroyHead(CallbackInfo ci) {
        this.custommusic$isDestroying = true;
    }

    @Inject(method = "destroy", at = @At("RETURN"))
    private void onDestroyReturn(CallbackInfo ci) {
        this.custommusic$isDestroying = false;
    }

    /**
     * Запобігає NullPointerException у tickNonPaused, якщо будь-який звук у instanceToChannel
     * не має запису в soundDeleteTime (наприклад, після переходу між світами).
     */
    @Inject(method = "tickNonPaused", at = @At("HEAD"))
    private void onTickNonPaused(CallbackInfo ci) {
        for (SoundInstance sound : this.instanceToChannel.keySet()) {
            this.soundDeleteTime.putIfAbsent(sound, this.tickCount + 20);
        }
    }

    @Inject(method = "stopAll", at = @At("HEAD"), cancellable = true)
    private void onStopAll(CallbackInfo ci) {
        if (!this.loaded) return;

        // При повному знищенні аудіорушія дозволяємо стандартне зупинення всього
        if (this.custommusic$isDestroying) {
            return;
        }

        SoundInstance currentMusic = CustomMusicPlayer.getInstance().getCurrentMusicInstance();
        if (currentMusic != null && this.instanceToChannel.containsKey(currentMusic)) {
            ci.cancel();

            this.executor.flush();

            // Зупиняємо всі звуки старого світу (моби, блоки, портали тощо), крім нашого музичного треку
            Iterator<Map.Entry<SoundInstance, ChannelAccess.ChannelHandle>> it = this.instanceToChannel.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> entry = it.next();
                if (entry.getKey() != currentMusic) {
                    entry.getValue().execute(Channel::stop);
                    it.remove();
                }
            }

            // Очищаємо інші колекції та черги світів
            this.queuedSounds.clear();
            this.tickingSounds.clear();
            this.instanceBySource.clear();
            this.instanceBySource.put(currentMusic.getSource(), currentMusic);

            // Очищаємо soundDeleteTime для всіх звуків, КРІМ нашої музики
            Iterator<Map.Entry<SoundInstance, Integer>> deleteIt = this.soundDeleteTime.entrySet().iterator();
            while (deleteIt.hasNext()) {
                if (deleteIt.next().getKey() != currentMusic) {
                    deleteIt.remove();
                }
            }
            this.soundDeleteTime.putIfAbsent(currentMusic, this.tickCount + 20);

            this.queuedTickableSounds.clear();
        }
    }
}
