package com.cat.custommusic.audio;

/**
 * Інтерфейс-розширення для каналу OpenAL Channel, що додає метод перемотки.
 */
public interface ISeekableChannel {
    /**
     * Перемотує поточний активний звук каналу на вказану секунду.
     * @param targetSeconds цільова секунда
     * @return true, якщо перемотка виконана успішно
     */
    boolean custommusic$seek(float targetSeconds);
}
