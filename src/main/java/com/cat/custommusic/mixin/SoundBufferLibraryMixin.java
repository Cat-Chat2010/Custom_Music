package com.cat.custommusic.mixin;

import com.cat.custommusic.CustomMusicMod;
import com.cat.custommusic.audio.Mp3AudioStream;
import com.cat.custommusic.audio.SeekableOggAudioStream;
import net.minecraft.Util;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.LoopingAudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
 * Mixin для SoundBufferLibrary:
 * Забезпечує створення аудіопотоків із підтримкою перемотки (SeekableAudioStream)
 * як для кастомних MP3/OGG, так і для ванільних музичних треків гри.
 */
@Mixin(SoundBufferLibrary.class)
public class SoundBufferLibraryMixin {
    @Shadow @Final private ResourceProvider resourceManager;

    @Inject(method = "getStream", at = @At("HEAD"), cancellable = true)
    private void onGetStream(ResourceLocation location, boolean isLooping, CallbackInfoReturnable<CompletableFuture<AudioStream>> cir) {
        if ("custommusic".equals(location.getNamespace())) {
            Path trackFile = CustomMusicMod.getInstance().getDirectoryManager().findTrackFileByLocation(location);
            if (trackFile != null && Files.isRegularFile(trackFile)) {
                String name = trackFile.getFileName().toString().toLowerCase(Locale.ROOT);
                if (name.endsWith(".mp3")) {
                    cir.setReturnValue(CompletableFuture.supplyAsync(() -> {
                        try {
                            if (isLooping) {
                                return new LoopingAudioStream(is -> {
                                    try {
                                        return new Mp3AudioStream(trackFile);
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                }, Files.newInputStream(trackFile));
                            } else {
                                return new Mp3AudioStream(trackFile);
                            }
                        } catch (Exception e) {
                            throw new CompletionException(e);
                        }
                    }, Util.nonCriticalIoPool()));
                } else if (name.endsWith(".ogg")) {
                    cir.setReturnValue(CompletableFuture.supplyAsync(() -> {
                        try {
                            return new SeekableOggAudioStream(() -> {
                                try {
                                    return Files.newInputStream(trackFile);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });
                        } catch (Exception e) {
                            throw new CompletionException(e);
                        }
                    }, Util.nonCriticalIoPool()));
                }
            }
        } else if ("minecraft".equals(location.getNamespace()) &&
                (location.getPath().startsWith("sounds/music/") || location.getPath().startsWith("sounds/records/"))) {
            // Для ванільної фонової музики та платівок Minecraft
            cir.setReturnValue(CompletableFuture.supplyAsync(() -> {
                try {
                    return new SeekableOggAudioStream(() -> {
                        try {
                            return this.resourceManager.open(location);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            }, Util.nonCriticalIoPool()));
        }
    }
}
