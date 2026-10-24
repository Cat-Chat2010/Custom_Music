package com.cat.custommusic.mixin;

import com.cat.custommusic.audio.ISeekableChannel;
import com.cat.custommusic.audio.SeekableAudioStream;
import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.sounds.AudioStream;
import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Channel.class)
public abstract class ChannelMixin implements ISeekableChannel {
    @Shadow private int source;
    @Shadow private AudioStream stream;
    @Shadow abstract void pumpBuffers(int count);

    @Override
    public boolean custommusic$seek(float targetSeconds) {
        if (this.stream == null) {
            return false;
        }

        try {
            // 1. Зупиняємо відтворення OpenAL джерела
            AL10.alSourceStop(this.source);

            // 2. Вилучаємо та видаляємо всі завантажені раніше буфери
            int queued = AL10.alGetSourcei(this.source, AL10.AL_BUFFERS_QUEUED);
            if (queued > 0) {
                int[] buffers = new int[queued];
                AL10.alSourceUnqueueBuffers(this.source, buffers);
                AL10.alDeleteBuffers(buffers);
            }

            // 3. Здійснюємо перемотку в самому потоці аудіо
            if (this.stream instanceof SeekableAudioStream seekable) {
                seekable.seekTo(targetSeconds);
            }

            // 4. Завантажуємо 4 нових буфери з нової позиції
            this.pumpBuffers(4);

            // 5. Відновлюємо звучання джерела OpenAL
            AL10.alSourcePlay(this.source);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
