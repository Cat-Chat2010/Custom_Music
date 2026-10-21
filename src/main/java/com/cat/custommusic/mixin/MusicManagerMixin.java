package com.cat.custommusic.mixin;

import com.cat.custommusic.audio.CustomMusicPlayer;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin для MusicManager, що замінює стандартну логіку планування та відтворення фонової музики.
 */
@Mixin(MusicManager.class)
public class MusicManagerMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        // Скасовуємо стандартне ванільне планування треків (відтворенням керує CustomMusicPlayer через ClientTickEvent)
        ci.cancel();
    }
}
