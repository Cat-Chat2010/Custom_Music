package com.cat.custommusic.audio;

import net.minecraft.client.sounds.AudioStream;
import java.io.IOException;

/**
 * Інтерфейс для аудіопотоків, що підтримують перемотку на потрібну секунду.
 */
public interface SeekableAudioStream extends AudioStream {
    /**
     * Перемотує потік на вказану секунду від початку треку.
     * @param targetSeconds цільова секунда
     */
    void seekTo(float targetSeconds) throws IOException;
}
