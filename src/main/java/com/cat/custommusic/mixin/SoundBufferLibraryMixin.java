package com.cat.custommusic.mixin;

import com.cat.custommusic.CustomMusicMod;
import com.cat.custommusic.audio.Mp3AudioStream;
import net.minecraft.Util;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.LoopingAudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Mixin для перехоплення завантаження аудіопотоку SoundBufferLibrary.
 * Дозволяє напряму відтворювати MP3 треки через OpenAL за допомогою читого Java декодера Mp3AudioStream.
 */
@Mixin(SoundBufferLibrary.class)
public class SoundBufferLibraryMixin {

    @Inject(method = "getStream", at = @At("HEAD"), cancellable = true)
    private void onGetStream(ResourceLocation location, boolean isLooping, CallbackInfoReturnable<CompletableFuture<AudioStream>> cir) {
        if ("custommusic".equals(location.getNamespace())) {
            Path trackFile = CustomMusicMod.getInstance().getDirectoryManager().findTrackFileByLocation(location);
            if (trackFile != null && Files.isRegularFile(trackFile)) {
                String name = trackFile.getFileName().toString().toLowerCase(Locale.ROOT);
                if (name.endsWith(".mp3")) {
                    cir.setReturnValue(CompletableFuture.supplyAsync(() -> {
                        try {
                            InputStream is = Files.newInputStream(trackFile);
                            if (isLooping) {
                                return new LoopingAudioStream(Mp3AudioStream::new, is);
                            } else {
                                return new Mp3AudioStream(is);
                            }
                        } catch (Exception e) {
                            throw new CompletionException(e);
                        }
                    }, Util.nonCriticalIoPool()));
                }
            }
        }
    }
}
