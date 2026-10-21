package com.cat.custommusic.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Конвертер аудіофайлів через зовнішній процес FFmpeg.
 */
public class FfmpegConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(FfmpegConverter.class);
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "CustomMusic-FFmpeg-Worker");
        thread.setDaemon(true);
        return thread;
    });

    private final MusicDirectoryManager directoryManager;
    private String resolvedFfmpegPath = null;
    private Boolean ffmpegAvailable = null;

    public FfmpegConverter(MusicDirectoryManager directoryManager) {
        this.directoryManager = directoryManager;
    }

    /**
     * Перевіряє, чи доступний FFmpeg (локально в custom_music/ffmpeg/ або в системному PATH).
     */
    public synchronized boolean isAvailable() {
        if (ffmpegAvailable != null) {
            return ffmpegAvailable;
        }

        // 1. Перевірка локальної папки custom_music/ffmpeg/
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String exeName = isWindows ? "ffmpeg.exe" : "ffmpeg";
        Path localFfmpeg = directoryManager.getFfmpegDir().resolve(exeName);

        if (Files.isRegularFile(localFfmpeg) && Files.isExecutable(localFfmpeg)) {
            if (testFfmpegExecutable(localFfmpeg.toAbsolutePath().toString())) {
                resolvedFfmpegPath = localFfmpeg.toAbsolutePath().toString();
                ffmpegAvailable = true;
                LOGGER.info("[CustomMusic] Знайдено локальний FFmpeg: {}", resolvedFfmpegPath);
                return true;
            }
        }

        // 2. Перевірка системного PATH
        if (testFfmpegExecutable(exeName)) {
            resolvedFfmpegPath = exeName;
            ffmpegAvailable = true;
            LOGGER.info("[CustomMusic] Знайдено системний FFmpeg у PATH");
            return true;
        }

        ffmpegAvailable = false;
        LOGGER.warn("[CustomMusic] FFmpeg не знайдено! Переконайтеся, що FFmpeg встановлений у системі (PATH) або додайте його у: {}",
                localFfmpeg.toAbsolutePath());
        return false;
    }

    private boolean testFfmpegExecutable(String command) {
        try {
            Process process = new ProcessBuilder(command, "-version")
                    .redirectErrorStream(true)
                    .start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Конвертує один аудіофайл у .ogg (Vorbis 44.1kHz Stereo).
     */
    public boolean convertFile(Path inputFile, Path outputOggFile) {
        if (!isAvailable()) {
            LOGGER.error("[CustomMusic] Неможливо конвертувати {}: FFmpeg відсутній!", inputFile.getFileName());
            return false;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    resolvedFfmpegPath,
                    "-y",
                    "-i", inputFile.toAbsolutePath().toString(),
                    "-c:a", "libvorbis",
                    "-ar", "44100",
                    "-ac", "2",
                    outputOggFile.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);

            LOGGER.info("[CustomMusic] Початок конвертації: {} -> {}", inputFile.getFileName(), outputOggFile.getFileName());
            Process process = pb.start();

            // Читаємо вивід процесу для логування у разі помилки
            StringBuilder outputLog = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputLog.append(line).append(System.lineSeparator());
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                LOGGER.info("[CustomMusic] Успішно сконвертовано: {}", outputOggFile.getFileName());
                // Видаляємо вхідний файл із папки import/
                Files.deleteIfExists(inputFile);
                LOGGER.info("[CustomMusic] Видалено оригінал з import/: {}", inputFile.getFileName());
                return true;
            } else {
                LOGGER.error("[CustomMusic] FFmpeg завершився з кодом {}. Лог:\n{}", exitCode, outputLog);
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("[CustomMusic] Помилка під час конвертації файлу: " + inputFile.getFileName(), e);
            return false;
        }
    }

    /**
     * Асинхронно сканує папку import/ і конвертує всі знайдені аудіофайли.
     */
    public CompletableFuture<Integer> convertAllImportsAsync(Consumer<String> progressConsumer) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAvailable()) {
                if (progressConsumer != null) {
                    progressConsumer.accept("FFmpeg не знайдено!");
                }
                return 0;
            }

            List<Path> importFiles = directoryManager.getImportFiles();
            if (importFiles.isEmpty()) {
                if (progressConsumer != null) {
                    progressConsumer.accept("Папка import/ порожня.");
                }
                return 0;
            }

            int convertedCount = 0;
            int total = importFiles.size();

            for (int i = 0; i < total; i++) {
                Path input = importFiles.get(i);
                String baseName = getBaseName(input.getFileName().toString());
                String sanitizedName = sanitizeFileName(baseName);
                Path output = directoryManager.getTracksDir().resolve(sanitizedName + ".ogg");

                // Якщо файл вже існує з таким іменем, підбираємо унікальне ім'я
                int counter = 1;
                while (Files.exists(output)) {
                    output = directoryManager.getTracksDir().resolve(sanitizedName + "_" + counter + ".ogg");
                    counter++;
                }

                if (progressConsumer != null) {
                    progressConsumer.accept(String.format("Конвертація (%d/%d): %s", i + 1, total, input.getFileName()));
                }

                boolean success = convertFile(input, output);
                if (success) {
                    convertedCount++;
                }
            }

            if (progressConsumer != null) {
                progressConsumer.accept(String.format("Завершено! Сконвертовано треків: %d з %d", convertedCount, total));
            }

            return convertedCount;
        }, EXECUTOR);
    }

    private String getBaseName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return (dot == -1) ? fileName : fileName.substring(0, dot);
    }

    private String sanitizeFileName(String name) {
        // Очищаємо ім'я файлу для коректного використання у ResourceLocation та файловій системі
        String sanitized = name.replaceAll("[^a-zA-Z0-9_.-]", "_").toLowerCase();
        if (sanitized.isEmpty()) {
            sanitized = "custom_track_" + System.currentTimeMillis();
        }
        return sanitized;
    }
}
