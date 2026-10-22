package com.cat.custommusic.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Утиліта для швидкого та точного визначення тривалості .ogg (Ogg Vorbis) файлів.
 * Зчитує sample rate з ідентифікаційного заголовка Vorbis
 * та granule position з останньої Ogg сторінки (кількість PCM семплів).
 * Працює напряму на чистому Java без залежностей від зовнішніх C бібліотек.
 */
public class OggAudioHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(OggAudioHelper.class);
    private static final Map<String, Integer> DURATION_CACHE = new ConcurrentHashMap<>();

    public static int getDurationInSeconds(Path oggPath) {
        if (oggPath == null || !Files.isRegularFile(oggPath)) {
            return 180;
        }

        String cacheKey = oggPath.toAbsolutePath().normalize().toString();
        Integer cached = DURATION_CACHE.get(cacheKey);
        if (cached != null && cached > 0) {
            return cached;
        }

        int calculatedSec = parseOggDuration(oggPath);
        if (calculatedSec > 0) {
            DURATION_CACHE.put(cacheKey, calculatedSec);
            LOGGER.info("[CustomMusic] Визначено тривалість {}: {} сек ({}хв {}с)",
                    oggPath.getFileName(), calculatedSec, calculatedSec / 60, calculatedSec % 60);
            return calculatedSec;
        }

        return 180;
    }

    private static int parseOggDuration(Path path) {
        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
            long fileLength = raf.length();
            if (fileLength < 64) {
                return 0;
            }

            // 1. Зчитуємо Sample Rate з початку файлу (перші 4 КБ)
            int sampleRate = 0;
            byte[] headerBuf = new byte[(int) Math.min(4096, fileLength)];
            raf.seek(0);
            raf.readFully(headerBuf);

            // Шукаємо сигнатуру "\x01vorbis" (Vorbis identification packet)
            byte[] vorbisSig = new byte[]{1, 'v', 'o', 'r', 'b', 'i', 's'};
            int vorbisIdx = indexOf(headerBuf, vorbisSig);
            if (vorbisIdx != -1 && vorbisIdx + 15 < headerBuf.length) {
                // За специфікацією Vorbis:
                // [0..6]: \x01vorbis
                // [7..10]: vorbis_version (4 bytes, uint32)
                // [11]: audio_channels (1 byte, uint8)
                // [12..15]: audio_sample_rate (4 bytes, uint32 little-endian)
                ByteBuffer bb = ByteBuffer.wrap(headerBuf, vorbisIdx + 12, 4).order(ByteOrder.LITTLE_ENDIAN);
                sampleRate = bb.getInt();
            }

            if (sampleRate <= 0) {
                sampleRate = 44100; // Стандартний fallback для OGG Vorbis
            }

            // 2. Шукаємо останню Ogg сторінку ("OggS") з кінця файлу
            // Шукаємо в останніх 64 КБ (розмір Ogg сторінки за специфікацією <= 65307 байт)
            int searchLen = (int) Math.min(65536, fileLength);
            long searchStart = fileLength - searchLen;
            raf.seek(searchStart);

            byte[] tailBuf = new byte[searchLen];
            raf.readFully(tailBuf);

            // Шукаємо "OggS" від кінця до початку
            byte[] oggSig = new byte[]{'O', 'g', 'g', 'S'};
            int lastOggIndex = lastIndexOf(tailBuf, oggSig);

            if (lastOggIndex != -1 && lastOggIndex + 14 <= tailBuf.length) {
                // [0..3]: OggS
                // [4]: version
                // [5]: header_type
                // [6..13]: granule_position (8 bytes, int64 little-endian)
                ByteBuffer bb = ByteBuffer.wrap(tailBuf, lastOggIndex + 6, 8).order(ByteOrder.LITTLE_ENDIAN);
                long granulePos = bb.getLong();

                // Якщо granule_position == -1 (неповний або continuation пакет), шукаємо попередню сторінку
                if (granulePos == -1) {
                    int prevOgg = lastIndexOfBefore(tailBuf, oggSig, lastOggIndex - 1);
                    if (prevOgg != -1 && prevOgg + 14 <= tailBuf.length) {
                        granulePos = ByteBuffer.wrap(tailBuf, prevOgg + 6, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
                    }
                }

                if (granulePos > 0 && sampleRate > 0) {
                    double seconds = (double) granulePos / (double) sampleRate;
                    return (int) Math.round(seconds);
                }
            }
        } catch (Throwable t) {
            LOGGER.warn("[CustomMusic] Не вдалося розпарсити OGG тривалість: " + path.getFileName(), t);
        }

        return 0;
    }

    private static final Map<String, Integer> KNOWN_VANILLA_DURATIONS = new java.util.HashMap<>();
    static {
        // C418 - Volume Alpha & Beta (Overworld / Creative / Menu / Nether / End)
        KNOWN_VANILLA_DURATIONS.put("calm1", 254);
        KNOWN_VANILLA_DURATIONS.put("calm2", 191);
        KNOWN_VANILLA_DURATIONS.put("calm3", 215);
        KNOWN_VANILLA_DURATIONS.put("subwoofer_lullaby", 208);
        KNOWN_VANILLA_DURATIONS.put("living_mice", 177);
        KNOWN_VANILLA_DURATIONS.put("haggstrom", 204);
        KNOWN_VANILLA_DURATIONS.put("danny", 254);
        KNOWN_VANILLA_DURATIONS.put("key", 65);
        KNOWN_VANILLA_DURATIONS.put("oxygene", 141);
        KNOWN_VANILLA_DURATIONS.put("dry_hands", 68);
        KNOWN_VANILLA_DURATIONS.put("wet_hands", 90);
        KNOWN_VANILLA_DURATIONS.put("mice_on_venus", 281);
        KNOWN_VANILLA_DURATIONS.put("hal1", 209);
        KNOWN_VANILLA_DURATIONS.put("hal2", 185);
        KNOWN_VANILLA_DURATIONS.put("hal3", 194);
        KNOWN_VANILLA_DURATIONS.put("hal4", 134);
        KNOWN_VANILLA_DURATIONS.put("nuance1", 110);
        KNOWN_VANILLA_DURATIONS.put("nuance2", 110);
        KNOWN_VANILLA_DURATIONS.put("piano1", 87);
        KNOWN_VANILLA_DURATIONS.put("piano2", 180);
        KNOWN_VANILLA_DURATIONS.put("piano3", 100);
        KNOWN_VANILLA_DURATIONS.put("mutation", 209);
        KNOWN_VANILLA_DURATIONS.put("moog_city_2", 180);
        KNOWN_VANILLA_DURATIONS.put("beginning_2", 175);
        KNOWN_VANILLA_DURATIONS.put("floating_trees", 244);
        KNOWN_VANILLA_DURATIONS.put("biome_fest", 378);
        KNOWN_VANILLA_DURATIONS.put("blind_spots", 332);
        KNOWN_VANILLA_DURATIONS.put("haunt_muskie", 361);
        KNOWN_VANILLA_DURATIONS.put("aria_math", 310);
        KNOWN_VANILLA_DURATIONS.put("dreiton", 497);
        KNOWN_VANILLA_DURATIONS.put("taswell", 455);
        KNOWN_VANILLA_DURATIONS.put("alpha", 603);
        KNOWN_VANILLA_DURATIONS.put("the_end", 600);
        KNOWN_VANILLA_DURATIONS.put("boss", 185);
        KNOWN_VANILLA_DURATIONS.put("credits", 604);

        // Nether Update (Lena Raine)
        KNOWN_VANILLA_DURATIONS.put("chrysopoeia", 303);
        KNOWN_VANILLA_DURATIONS.put("rubedo", 312);
        KNOWN_VANILLA_DURATIONS.put("so_below", 319);

        // Caves & Cliffs / Wild Update / Trails & Tales / Tricky Trials
        KNOWN_VANILLA_DURATIONS.put("stand_tall", 308);
        KNOWN_VANILLA_DURATIONS.put("left_to_bloom", 342);
        KNOWN_VANILLA_DURATIONS.put("wending", 314);
        KNOWN_VANILLA_DURATIONS.put("infinite_amethyst", 271);
        KNOWN_VANILLA_DURATIONS.put("one_more_day", 278);
        KNOWN_VANILLA_DURATIONS.put("ancestry", 343);
        KNOWN_VANILLA_DURATIONS.put("floating_dream", 205);
        KNOWN_VANILLA_DURATIONS.put("comforting_memories", 275);
        KNOWN_VANILLA_DURATIONS.put("an_ordinary_day", 331);
        KNOWN_VANILLA_DURATIONS.put("echo_in_the_canyon", 304);
        KNOWN_VANILLA_DURATIONS.put("echo_in_the_wind", 304);
        KNOWN_VANILLA_DURATIONS.put("a_familiar_room", 241);
        KNOWN_VANILLA_DURATIONS.put("bromeliad", 312);
        KNOWN_VANILLA_DURATIONS.put("relic", 218);
        KNOWN_VANILLA_DURATIONS.put("creator", 177);
        KNOWN_VANILLA_DURATIONS.put("creator_music_box", 74);
        KNOWN_VANILLA_DURATIONS.put("precipice", 299);
        KNOWN_VANILLA_DURATIONS.put("deeper", 303);
        KNOWN_VANILLA_DURATIONS.put("eld_bomb", 334);
        KNOWN_VANILLA_DURATIONS.put("featherfall", 344);
        KNOWN_VANILLA_DURATIONS.put("endless", 402);
        KNOWN_VANILLA_DURATIONS.put("komorebi", 287);
        KNOWN_VANILLA_DURATIONS.put("pokopoko", 314);
        KNOWN_VANILLA_DURATIONS.put("yakusoku", 271);
        KNOWN_VANILLA_DURATIONS.put("axolotl", 303);
        KNOWN_VANILLA_DURATIONS.put("dragon_fish", 192);
        KNOWN_VANILLA_DURATIONS.put("shuniji", 244);
    }

    private static final Map<String, String> VANILLA_TRACK_NAMES = new java.util.HashMap<>();
    static {
        VANILLA_TRACK_NAMES.put("calm1", "C418 - Minecraft");
        VANILLA_TRACK_NAMES.put("calm2", "C418 - Clark");
        VANILLA_TRACK_NAMES.put("calm3", "C418 - Sweden");
        VANILLA_TRACK_NAMES.put("subwoofer_lullaby", "C418 - Subwoofer Lullaby");
        VANILLA_TRACK_NAMES.put("living_mice", "C418 - Living Mice");
        VANILLA_TRACK_NAMES.put("haggstrom", "C418 - Haggstrom");
        VANILLA_TRACK_NAMES.put("danny", "C418 - Danny");
        VANILLA_TRACK_NAMES.put("key", "C418 - Key");
        VANILLA_TRACK_NAMES.put("oxygene", "C418 - Oxygène");
        VANILLA_TRACK_NAMES.put("dry_hands", "C418 - Dry Hands");
        VANILLA_TRACK_NAMES.put("wet_hands", "C418 - Wet Hands");
        VANILLA_TRACK_NAMES.put("mice_on_venus", "C418 - Mice on Venus");
        VANILLA_TRACK_NAMES.put("hal1", "C418 - Hal 1");
        VANILLA_TRACK_NAMES.put("hal2", "C418 - Hal 2");
        VANILLA_TRACK_NAMES.put("hal3", "C418 - Hal 3");
        VANILLA_TRACK_NAMES.put("hal4", "C418 - Hal 4");
        VANILLA_TRACK_NAMES.put("nuance1", "C418 - Nuance 1");
        VANILLA_TRACK_NAMES.put("nuance2", "C418 - Nuance 2");
        VANILLA_TRACK_NAMES.put("piano1", "C418 - Chris");
        VANILLA_TRACK_NAMES.put("piano2", "C418 - Piano 2");
        VANILLA_TRACK_NAMES.put("piano3", "C418 - Piano 3");
        VANILLA_TRACK_NAMES.put("mutation", "C418 - Mutation");
        VANILLA_TRACK_NAMES.put("moog_city_2", "C418 - Moog City 2");
        VANILLA_TRACK_NAMES.put("beginning_2", "C418 - Beginning 2");
        VANILLA_TRACK_NAMES.put("floating_trees", "C418 - Floating Trees");
        VANILLA_TRACK_NAMES.put("biome_fest", "C418 - Biome Fest");
        VANILLA_TRACK_NAMES.put("blind_spots", "C418 - Blind Spots");
        VANILLA_TRACK_NAMES.put("haunt_muskie", "C418 - Haunt Muskie");
        VANILLA_TRACK_NAMES.put("aria_math", "C418 - Aria Math");
        VANILLA_TRACK_NAMES.put("dreiton", "C418 - Dreiton");
        VANILLA_TRACK_NAMES.put("taswell", "C418 - Taswell");
        VANILLA_TRACK_NAMES.put("alpha", "C418 - Alpha");
        VANILLA_TRACK_NAMES.put("the_end", "C418 - The End");
        VANILLA_TRACK_NAMES.put("boss", "C418 - Boss (Dragon)");
        VANILLA_TRACK_NAMES.put("credits", "C418 - Credits");

        // Nether
        VANILLA_TRACK_NAMES.put("chrysopoeia", "Lena Raine - Chrysopoeia");
        VANILLA_TRACK_NAMES.put("rubedo", "Lena Raine - Rubedo");
        VANILLA_TRACK_NAMES.put("so_below", "Lena Raine - So Below");

        // Updates
        VANILLA_TRACK_NAMES.put("stand_tall", "Lena Raine - Stand Tall");
        VANILLA_TRACK_NAMES.put("left_to_bloom", "Lena Raine - Left to Bloom");
        VANILLA_TRACK_NAMES.put("wending", "Lena Raine - Wending");
        VANILLA_TRACK_NAMES.put("infinite_amethyst", "Lena Raine - Infinite Amethyst");
        VANILLA_TRACK_NAMES.put("one_more_day", "Lena Raine - One More Day");
        VANILLA_TRACK_NAMES.put("ancestry", "Lena Raine - Ancestry");
        VANILLA_TRACK_NAMES.put("floating_dream", "Kumi Tanioka - Floating Dream");
        VANILLA_TRACK_NAMES.put("comforting_memories", "Kumi Tanioka - Comforting Memories");
        VANILLA_TRACK_NAMES.put("an_ordinary_day", "Kumi Tanioka - An Ordinary Day");
        VANILLA_TRACK_NAMES.put("echo_in_the_canyon", "Aaron Cherof - Echo in the Wind");
        VANILLA_TRACK_NAMES.put("echo_in_the_wind", "Aaron Cherof - Echo in the Wind");
        VANILLA_TRACK_NAMES.put("a_familiar_room", "Aaron Cherof - A Familiar Room");
        VANILLA_TRACK_NAMES.put("bromeliad", "Aaron Cherof - Bromeliad");
        VANILLA_TRACK_NAMES.put("relic", "Aaron Cherof - Relic");
        VANILLA_TRACK_NAMES.put("creator", "Lena Raine - Creator");
        VANILLA_TRACK_NAMES.put("creator_music_box", "Lena Raine - Creator (Music Box)");
        VANILLA_TRACK_NAMES.put("precipice", "Aaron Cherof - Precipice");
        VANILLA_TRACK_NAMES.put("deeper", "Lena Raine - Deeper");
        VANILLA_TRACK_NAMES.put("eld_bomb", "Lena Raine - Eld Bomb");
        VANILLA_TRACK_NAMES.put("featherfall", "Aaron Cherof - Featherfall");
        VANILLA_TRACK_NAMES.put("endless", "Lena Raine - Endless");
        VANILLA_TRACK_NAMES.put("komorebi", "Kumi Tanioka - Komorebi");
        VANILLA_TRACK_NAMES.put("pokopoko", "Kumi Tanioka - Poko Poko");
        VANILLA_TRACK_NAMES.put("yakusoku", "Kumi Tanioka - Yakusoku");
        VANILLA_TRACK_NAMES.put("axolotl", "C418 - Axolotl");
        VANILLA_TRACK_NAMES.put("dragon_fish", "C418 - Dragon Fish");
        VANILLA_TRACK_NAMES.put("shuniji", "C418 - Shuniji");
    }

    public static String getResolvedVanillaTrackName(net.minecraft.client.resources.sounds.SoundInstance instance) {
        if (instance != null && instance.getSound() != null && instance.getSound().getLocation() != null) {
            String path = instance.getSound().getLocation().getPath().toLowerCase(java.util.Locale.ROOT);
            for (Map.Entry<String, String> entry : VANILLA_TRACK_NAMES.entrySet()) {
                if (path.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Визначає точну тривалість ванільного треку Minecraft за допомогою ResourceManager або вбудованої бази даних.
     */
    public static int getVanillaTrackDuration(net.minecraft.client.Minecraft mc, net.minecraft.client.resources.sounds.SoundInstance instance, com.cat.custommusic.config.TrackEntry track) {
        String resolvedSoundName = null;
        if (instance != null && mc != null) {
            try {
                if (instance.getSound() == null) {
                    instance.resolve(mc.getSoundManager());
                }
                net.minecraft.client.resources.sounds.Sound sound = instance.getSound();
                if (sound != null) {
                    if (sound.getLocation() != null) {
                        resolvedSoundName = sound.getLocation().getPath().toLowerCase(java.util.Locale.ROOT);
                    }

                    net.minecraft.resources.ResourceLocation soundLoc = sound.getPath();
                    java.util.Optional<net.minecraft.server.packs.resources.Resource> res = mc.getResourceManager().getResource(soundLoc);
                    if (!res.isPresent()) {
                        soundLoc = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(sound.getLocation().getNamespace(), "sounds/" + sound.getLocation().getPath() + ".ogg");
                        res = mc.getResourceManager().getResource(soundLoc);
                    }

                    if (res.isPresent()) {
                        try (java.io.InputStream is = res.get().open()) {
                            byte[] bytes = is.readAllBytes();
                            int dur = getDurationFromBytes(bytes);
                            if (dur > 0) {
                                LOGGER.info("[CustomMusic] Визначено точну тривалість ванільного треку [{}]: {} сек ({}хв {}с)",
                                        soundLoc, dur, dur / 60, dur % 60);
                                return dur;
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                LOGGER.warn("[CustomMusic] Не вдалося зчитати ванільний звук через ResourceManager", t);
            }
        }

        // Перевіряємо за назвою конкретно обраного звуку (наприклад "music/game/wet_hands" -> 90s)
        if (resolvedSoundName != null) {
            for (Map.Entry<String, Integer> entry : KNOWN_VANILLA_DURATIONS.entrySet()) {
                if (resolvedSoundName.contains(entry.getKey())) {
                    LOGGER.info("[CustomMusic] Точна тривалість ванільного треку [{}] зі словника: {} сек", resolvedSoundName, entry.getValue());
                    return entry.getValue();
                }
            }
        }

        // Категорійні орієнтири для ванільних мелодій якщо конкретний файл не знайдено
        if (track != null && track.getId() != null) {
            String id = track.getId().toLowerCase(java.util.Locale.ROOT);
            for (Map.Entry<String, Integer> entry : KNOWN_VANILLA_DURATIONS.entrySet()) {
                if (id.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }

            if (id.contains("creative")) return 310;
            if (id.contains("credits")) return 604;
            if (id.contains("under_water")) return 244;
            if (id.contains("end") && !id.contains("dragon")) return 600;
            if (id.contains("dragon")) return 185;
            if (id.contains("nether")) return 312;
            if (id.contains("menu")) return 180;
            if (id.contains("game")) return 215;
        }

        return 180;
    }

    /**
     * Визначає тривалість треку (кастомного або ванільного) у секундах.
     */
    public static int getTrackDurationSeconds(com.cat.custommusic.config.TrackEntry track) {
        if (track == null) return 180;
        if (track.isCustom()) {
            java.nio.file.Path tracksDir = com.cat.custommusic.CustomMusicMod.getInstance().getDirectoryManager().getTracksDir();
            String fn = track.getFileName() != null ? track.getFileName() : "";
            java.nio.file.Path oggPath = tracksDir.resolve(fn);
            if (!java.nio.file.Files.exists(oggPath) && !fn.toLowerCase(java.util.Locale.ROOT).endsWith(".ogg")) {
                oggPath = tracksDir.resolve(fn + ".ogg");
            }
            return getDurationInSeconds(oggPath);
        } else {
            com.cat.custommusic.config.VanillaTracks.VanillaTrackInfo info = com.cat.custommusic.config.VanillaTracks.findByKey(track.getId());
            if (info != null) {
                return info.durationSeconds();
            }
            return getVanillaTrackDuration(net.minecraft.client.Minecraft.getInstance(), null, track);
        }
    }

    /**
     * Повертає відформатовану тривалість треку (наприклад "3:24" або "1:30").
     */
    public static String getTrackDurationFormatted(com.cat.custommusic.config.TrackEntry track) {
        int sec = getTrackDurationSeconds(track);
        return CustomMusicPlayer.formatDuration(sec);
    }

    /**
     * Визначає тривалість OGG аудіо безпосередньо з байтового масиву.
     */
    public static int getDurationFromBytes(byte[] data) {
        if (data == null || data.length < 64) {
            return 0;
        }

        try {
            int fileLength = data.length;

            // 1. Sample rate з початку файлу
            int sampleRate = 0;
            int headerLen = Math.min(4096, fileLength);
            byte[] vorbisSig = new byte[]{1, 'v', 'o', 'r', 'b', 'i', 's'};
            int vorbisIdx = indexOf(data, vorbisSig);
            if (vorbisIdx != -1 && vorbisIdx + 15 < fileLength) {
                ByteBuffer bb = ByteBuffer.wrap(data, vorbisIdx + 12, 4).order(ByteOrder.LITTLE_ENDIAN);
                sampleRate = bb.getInt();
            }

            if (sampleRate <= 0) {
                sampleRate = 44100;
            }

            // 2. Шукаємо останню сторінку "OggS" з кінця файлу
            byte[] oggSig = new byte[]{'O', 'g', 'g', 'S'};
            int searchLen = Math.min(65536, fileLength);
            int searchStart = fileLength - searchLen;

            byte[] tail = new byte[searchLen];
            System.arraycopy(data, searchStart, tail, 0, searchLen);

            int lastOggIndex = lastIndexOf(tail, oggSig);
            if (lastOggIndex != -1 && lastOggIndex + 14 <= tail.length) {
                ByteBuffer bb = ByteBuffer.wrap(tail, lastOggIndex + 6, 8).order(ByteOrder.LITTLE_ENDIAN);
                long granulePos = bb.getLong();

                if (granulePos == -1) {
                    int prevOgg = lastIndexOfBefore(tail, oggSig, lastOggIndex - 1);
                    if (prevOgg != -1 && prevOgg + 14 <= tail.length) {
                        granulePos = ByteBuffer.wrap(tail, prevOgg + 6, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
                    }
                }

                if (granulePos > 0 && sampleRate > 0) {
                    double seconds = (double) granulePos / (double) sampleRate;
                    return (int) Math.round(seconds);
                }
            }
        } catch (Throwable t) {
            LOGGER.warn("[CustomMusic] Не вдалося розпарсити тривалість з масиву байтів", t);
        }

        return 0;
    }

    private static int indexOf(byte[] data, byte[] pattern) {
        outer:
        for (int i = 0; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static int lastIndexOf(byte[] data, byte[] pattern) {
        return lastIndexOfBefore(data, pattern, data.length - pattern.length);
    }

    private static int lastIndexOfBefore(byte[] data, byte[] pattern, int maxStartIndex) {
        outer:
        for (int i = Math.min(maxStartIndex, data.length - pattern.length); i >= 0; i--) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
