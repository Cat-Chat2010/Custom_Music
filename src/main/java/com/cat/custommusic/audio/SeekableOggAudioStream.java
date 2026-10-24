package com.cat.custommusic.audio;

import net.minecraft.client.sounds.JOrbisAudioStream;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.function.Supplier;

/**
 * Високопродуктивна обгортка для JOrbisAudioStream з миттєвою апаратною перемоткою:
 * - Циклічний буфер історії (30 секунд PCM) для миттєвої перемотки назад (-5с) без повторного відкриття та без декодування (0 мс, 0% CPU).
 * - Швидка перемотка вперед (+5с) без перезавантаження файлу.
 * - Коректне визначення EOF (повернення null при кінці треку), що запобігає зацикленню порожніх буферів в OpenAL.
 */
public class SeekableOggAudioStream implements SeekableAudioStream {
    private final Supplier<InputStream> streamSupplier;
    private JOrbisAudioStream delegate;
    private final AudioFormat format;
    private final int frameSize;
    private final int historyCapacity;
    private final byte[] history;
    private long totalDecodedBytes = 0;
    private long currentStreamBytes = 0;

    public SeekableOggAudioStream(Supplier<InputStream> streamSupplier) throws IOException {
        this.streamSupplier = streamSupplier;
        this.delegate = new JOrbisAudioStream(new BufferedInputStream(streamSupplier.get(), 65536));
        this.format = this.delegate.getFormat();

        int ch = format.getChannels();
        int bits = format.getSampleSizeInBits();
        this.frameSize = (ch > 0 && bits > 0) ? (ch * bits / 8) : 4;

        float sampleRate = format.getSampleRate() > 0 ? format.getSampleRate() : 44100.0f;
        int bytesPerSec = (int) (sampleRate * this.frameSize);
        // Зберігаємо 30 секунд історії декодованого PCM звуку (~5 МБ)
        this.historyCapacity = Math.max(bytesPerSec * 30, 65536);
        this.history = new byte[this.historyCapacity];
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }

    @Override
    public synchronized ByteBuffer read(int size) throws IOException {
        // 1. Якщо відтворюємо з буфера історії після перемотки назад (-5с)
        if (currentStreamBytes < totalDecodedBytes) {
            int available = (int) (totalDecodedBytes - currentStreamBytes);
            int toRead = Math.min(size, available);
            if (frameSize > 0) {
                toRead = (toRead / frameSize) * frameSize;
            }
            if (toRead <= 0) {
                toRead = Math.min(size, available);
            }

            ByteBuffer outBuf = BufferUtils.createByteBuffer(toRead);
            int readPos = (int) (currentStreamBytes % historyCapacity);
            int firstPart = Math.min(toRead, historyCapacity - readPos);
            outBuf.put(history, readPos, firstPart);
            if (toRead > firstPart) {
                outBuf.put(history, 0, toRead - firstPart);
            }
            outBuf.flip();
            currentStreamBytes += toRead;
            return outBuf;
        }

        // 2. Читаємо нові дані з JOrbisAudioStream
        ByteBuffer buf = delegate.read(size);
        if (buf == null || !buf.hasRemaining()) {
            // КРИТИЧНО: повертаємо null, щоб OpenAL Channel знав про кінець файлу
            // і не відправляв порожні 0-байтні буфери по колу!
            return null;
        }

        int len = buf.remaining();
        int mark = buf.position();

        // Записуємо прочитані PCM байти в кільцевий буфер історії
        if (len >= historyCapacity) {
            int skip = len - historyCapacity;
            buf.position(mark + skip);
            buf.get(history, 0, historyCapacity);
            buf.position(mark);
        } else {
            int writePos = (int) (totalDecodedBytes % historyCapacity);
            int firstPart = Math.min(len, historyCapacity - writePos);
            buf.get(history, writePos, firstPart);
            if (len > firstPart) {
                buf.get(history, 0, len - firstPart);
            }
            buf.position(mark);
        }

        totalDecodedBytes += len;
        currentStreamBytes += len;
        return buf;
    }

    @Override
    public synchronized void close() throws IOException {
        delegate.close();
    }

    @Override
    public synchronized void seekTo(float targetSeconds) throws IOException {
        long targetBytes = (long) (format.getSampleRate() * format.getChannels() * (format.getSampleSizeInBits() / 8.0f) * targetSeconds);
        if (frameSize > 0) {
            targetBytes = (targetBytes / frameSize) * frameSize;
        }

        long minHistoryByte = Math.max(0, totalDecodedBytes - historyCapacity);

        // Сценарій 1: Цільова позиція вже є в кільцевому буфері пам'яті (перемотка назад -5с або вперед у вже кешоване)
        // Миттєвий перехід (0 мс) без декодування та без зависань рушія!
        if (targetBytes >= minHistoryByte && targetBytes <= totalDecodedBytes) {
            this.currentStreamBytes = targetBytes;
            return;
        }

        // Сценарій 2: Перемотка вперед на невелику відстань (до 30 с попереду)
        if (targetBytes > totalDecodedBytes && (targetBytes - totalDecodedBytes) <= 30L * 176400L) {
            this.currentStreamBytes = totalDecodedBytes;
            long bytesToSkip = targetBytes - totalDecodedBytes;
            skipBytes(bytesToSkip);
            return;
        }

        // Сценарій 3: Стрибок далеко назад поза межами буфера історії (наприклад, перемотка з кінця на самий початок)
        try {
            delegate.close();
        } catch (Exception ignored) {
        }
        this.delegate = new JOrbisAudioStream(new BufferedInputStream(streamSupplier.get(), 65536));
        this.totalDecodedBytes = 0;
        this.currentStreamBytes = 0;
        skipBytes(targetBytes);
    }

    private void skipBytes(long bytesToSkip) throws IOException {
        while (bytesToSkip > 0) {
            int chunk = (int) Math.min(bytesToSkip, 65536L);
            ByteBuffer buf = delegate.read(chunk);
            if (buf == null || !buf.hasRemaining()) {
                break;
            }
            int len = buf.remaining();
            int mark = buf.position();

            if (len >= historyCapacity) {
                int skip = len - historyCapacity;
                buf.position(mark + skip);
                buf.get(history, 0, historyCapacity);
                buf.position(mark);
            } else {
                int writePos = (int) (totalDecodedBytes % historyCapacity);
                int firstPart = Math.min(len, historyCapacity - writePos);
                buf.get(history, writePos, firstPart);
                if (len > firstPart) {
                    buf.get(history, 0, len - firstPart);
                }
                buf.position(mark);
            }

            totalDecodedBytes += len;
            currentStreamBytes += len;
            bytesToSkip -= len;
        }
    }
}
