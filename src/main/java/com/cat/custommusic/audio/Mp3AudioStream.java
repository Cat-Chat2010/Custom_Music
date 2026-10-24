package com.cat.custommusic.audio;

import javazoom.jl.decoder.*;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Аудіопотік для відтворення MP3 треків безпосередньо через OpenAL у Minecraft.
 * Використовує чистий Java декодер JLayer для декодування MP3 фреймів у 16-бітний PCM
 * та циклічний буфер історії (30 секунд) для миттєвої перемотки назад (-5с) без повторного декодування.
 */
public class Mp3AudioStream implements SeekableAudioStream {
    private final Supplier<InputStream> streamSupplier;
    private Bitstream bitstream;
    private Decoder decoder;
    private final AudioFormat format;
    private final int frameSize;
    private boolean eos = false;

    // Циклічний буфер історії (30 секунд PCM) для миттєвої перемотки назад (-5с)
    private final int historyCapacity;
    private final byte[] history;
    private long totalDecodedBytes = 0;
    private long currentStreamBytes = 0;

    // Внутрішній буфер декодованих PCM байтів
    private byte[] buffer = new byte[65536];
    private int bufferPos = 0;
    private int bufferCount = 0;

    public Mp3AudioStream(Path filePath) throws IOException {
        this(() -> {
            try {
                return Files.newInputStream(filePath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public Mp3AudioStream(InputStream inputStream) throws IOException {
        this(() -> inputStream);
    }

    public Mp3AudioStream(Supplier<InputStream> streamSupplier) throws IOException {
        this.streamSupplier = streamSupplier;
        InputStream buffered = new BufferedInputStream(streamSupplier.get(), 32768);
        this.bitstream = new Bitstream(buffered);
        this.decoder = new Decoder();

        // Декодуємо перший фрейм для визначення точного AudioFormat (частота, канали)
        try {
            Header header = bitstream.readFrame();
            if (header != null) {
                SampleBuffer sampleBuffer = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                int sampleRate = sampleBuffer.getSampleFrequency();
                int channels = sampleBuffer.getChannelCount();
                this.format = new AudioFormat((float) sampleRate, 16, channels, true, false);

                appendSamples(sampleBuffer);
                bitstream.closeFrame();
            } else {
                this.eos = true;
                this.format = new AudioFormat(44100.0f, 16, 2, true, false);
            }
        } catch (BitstreamException | DecoderException e) {
            throw new IOException("Failed to initialize MP3 decoder", e);
        }

        int ch = this.format.getChannels();
        int bits = this.format.getSampleSizeInBits();
        this.frameSize = (ch > 0 && bits > 0) ? (ch * bits / 8) : 4;
        float sampleRate = this.format.getSampleRate() > 0 ? this.format.getSampleRate() : 44100.0f;
        int bytesPerSec = (int) (sampleRate * this.frameSize);
        this.historyCapacity = Math.max(bytesPerSec * 30, 65536);
        this.history = new byte[this.historyCapacity];
    }

    @Override
    public synchronized void seekTo(float targetSeconds) throws IOException {
        long targetBytes = (long) (format.getSampleRate() * format.getChannels() * (format.getSampleSizeInBits() / 8.0f) * targetSeconds);
        if (frameSize > 0) {
            targetBytes = (targetBytes / frameSize) * frameSize;
        }

        long minHistoryByte = Math.max(0, totalDecodedBytes - historyCapacity);

        // Сценарій 1: Цільова позиція вже є в буфері історії (миттєвий -5с стрибок)
        if (targetBytes >= minHistoryByte && targetBytes <= totalDecodedBytes) {
            this.currentStreamBytes = targetBytes;
            return;
        }

        if (this.streamSupplier == null) return;
        try {
            this.bitstream.close();
        } catch (Exception ignored) {
        }

        InputStream buffered = new BufferedInputStream(this.streamSupplier.get(), 32768);
        this.bitstream = new Bitstream(buffered);
        this.decoder = new Decoder();
        this.bufferPos = 0;
        this.bufferCount = 0;
        this.eos = false;
        this.totalDecodedBytes = 0;
        this.currentStreamBytes = 0;

        float elapsed = 0.0f;
        while (elapsed < targetSeconds) {
            try {
                Header header = this.bitstream.readFrame();
                if (header == null) {
                    this.eos = true;
                    break;
                }
                elapsed += header.ms_per_frame() / 1000.0f;
                this.bitstream.closeFrame();
            } catch (Exception e) {
                this.eos = true;
                break;
            }
        }
    }

    private void appendSamples(SampleBuffer sampleBuffer) {
        short[] pcm = sampleBuffer.getBuffer();
        int len = sampleBuffer.getBufferLength();
        int neededBytes = len * 2;

        ensureCapacity(bufferCount + neededBytes);

        // 16-бітні семпли PCM (Little Endian: молодший байт, потім старший)
        for (int i = 0; i < len; i++) {
            short s = pcm[i];
            buffer[bufferCount++] = (byte) (s & 0xFF);
            buffer[bufferCount++] = (byte) ((s >> 8) & 0xFF);
        }
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity > buffer.length) {
            int newCap = Math.max(buffer.length * 2, minCapacity);
            byte[] newBuf = new byte[newCap];
            int available = bufferCount - bufferPos;
            if (available > 0) {
                System.arraycopy(buffer, bufferPos, newBuf, 0, available);
            }
            buffer = newBuf;
            bufferPos = 0;
            bufferCount = available;
        } else if (bufferPos > 0 && bufferCount + 8192 > buffer.length) {
            int available = bufferCount - bufferPos;
            if (available > 0) {
                System.arraycopy(buffer, bufferPos, buffer, 0, available);
            }
            bufferPos = 0;
            bufferCount = available;
        }
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }

    @Override
    public synchronized ByteBuffer read(int size) throws IOException {
        // 1. Відтворюємо з буфера історії, якщо ми відмотали назад
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

        // 2. Якщо в буфері менше даних ніж запитано, додекодовуємо фрейми з MP3
        while (!eos && (bufferCount - bufferPos) < size) {
            try {
                Header header = bitstream.readFrame();
                if (header == null) {
                    eos = true;
                    break;
                }
                SampleBuffer sampleBuffer = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                appendSamples(sampleBuffer);
                bitstream.closeFrame();
            } catch (BitstreamException | DecoderException e) {
                // При пошкодженому фреймі або завершенні файлу зупиняємо декодування
                eos = true;
                break;
            }
        }

        int available = bufferCount - bufferPos;
        if (available <= 0) {
            return null; // Кінець аудіопотоку
        }

        int bytesToRead = Math.min(available, size);
        ByteBuffer byteBuf = BufferUtils.createByteBuffer(bytesToRead);
        byteBuf.put(buffer, bufferPos, bytesToRead);
        byteBuf.flip();

        // Записуємо байти в кільцевий буфер історії для миттєвої перемотки назад
        if (bytesToRead >= historyCapacity) {
            int skip = bytesToRead - historyCapacity;
            System.arraycopy(buffer, bufferPos + skip, history, 0, historyCapacity);
        } else {
            int writePos = (int) (totalDecodedBytes % historyCapacity);
            int firstPart = Math.min(bytesToRead, historyCapacity - writePos);
            System.arraycopy(buffer, bufferPos, history, writePos, firstPart);
            if (bytesToRead > firstPart) {
                System.arraycopy(buffer, bufferPos + firstPart, history, 0, bytesToRead - firstPart);
            }
        }

        totalDecodedBytes += bytesToRead;
        currentStreamBytes += bytesToRead;

        bufferPos += bytesToRead;
        if (bufferPos == bufferCount) {
            bufferPos = 0;
            bufferCount = 0;
        }

        return byteBuf;
    }

    @Override
    public synchronized void close() throws IOException {
        try {
            bitstream.close();
        } catch (BitstreamException e) {
            throw new IOException("Failed to close MP3 bitstream", e);
        }
    }
}
