package com.cat.custommusic.mixin;

import com.cat.custommusic.audio.CustomMusicPlayer;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin для SoundManager, що запобігає замовканню фонової музики при відкритті меню та налаштувань.
 */
@Mixin(SoundManager.class)
public class SoundManagerMixin {

    @Inject(method = "pause", at = @At("TAIL"))
    private void onPause(CallbackInfo ci) {
        // Якщо грає музика і вона не на паузі користувачем, продовжуємо відтворення в налаштуваннях гри
        if (CustomMusicPlayer.getInstance().isPlaying() && !CustomMusicPlayer.getInstance().isPaused()) {
            ((SoundManager) (Object) this).resume();
        }
    }
}
