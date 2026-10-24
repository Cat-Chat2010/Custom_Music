package com.cat.custommusic.audio;

import javazoom.jl.decoder.*;
import net.minecraft.client.sounds.AudioStream;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Аудіопотік для відтворення MP3 треків безпосередньо через OpenAL у Minecraft.
 * Використовує чистий Java декодер JLayer для декодування MP3 фреймів у 16-бітний PCM.
 */
public class Mp3AudioStream implements AudioStream {
    private final Bitstream bitstream;
    private final Decoder decoder;
    private final AudioFormat format;
    private boolean eos = false;

    // Внутрішній буфер декодованих PCM байтів
    private byte[] buffer = new byte[65536];
    private int bufferPos = 0;
    private int bufferCount = 0;

    public Mp3AudioStream(InputStream inputStream) throws IOException {
        InputStream buffered = (inputStream instanceof BufferedInputStream)
                ? inputStream
                : new BufferedInputStream(inputStream, 32768);
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
    public ByteBuffer read(int size) throws IOException {
        // Якщо в буфері менше даних ніж запитано, додекодовуємо фрейми з MP3
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

        bufferPos += bytesToRead;
        if (bufferPos == bufferCount) {
            bufferPos = 0;
            bufferCount = 0;
        }

        return byteBuf;
    }

    @Override
    public void close() throws IOException {
        try {
            bitstream.close();
        } catch (BitstreamException e) {
            throw new IOException("Failed to close MP3 bitstream", e);
        }
    }
}
