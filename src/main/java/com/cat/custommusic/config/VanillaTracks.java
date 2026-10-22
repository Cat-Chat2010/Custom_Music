package com.cat.custommusic.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Довідковий список конкретних ванільних треків Minecraft з фіксованими шляхами до звуків.
 * Кожен трек відтворює саме обрану мелодію без випадкового вибору.
 */
public class VanillaTracks {

    public record VanillaTrackInfo(String key, String displayName, String soundPath, int durationSeconds) {}

    public static final List<VanillaTrackInfo> ALL_VANILLA_TRACKS = new ArrayList<>();

    static {
        // C418 - Класичні треки виживання (Overworld)
        def("v_sweden", "C418 - Sweden", "minecraft:music/game/sweden", 215);
        def("v_minecraft", "C418 - Minecraft", "minecraft:music/game/minecraft", 254);
        def("v_clark", "C418 - Clark", "minecraft:music/game/clark", 191);
        def("v_wet_hands", "C418 - Wet Hands", "minecraft:music/game/wet_hands", 90);
        def("v_dry_hands", "C418 - Dry Hands", "minecraft:music/game/dry_hands", 68);
        def("v_subwoofer_lullaby", "C418 - Subwoofer Lullaby", "minecraft:music/game/subwoofer_lullaby", 208);
        def("v_living_mice", "C418 - Living Mice", "minecraft:music/game/living_mice", 177);
        def("v_haggstrom", "C418 - Haggstrom", "minecraft:music/game/haggstrom", 204);
        def("v_danny", "C418 - Danny", "minecraft:music/game/danny", 254);
        def("v_mice_on_venus", "C418 - Mice on Venus", "minecraft:music/game/mice_on_venus", 281);
        def("v_key", "C418 - Key", "minecraft:music/game/key", 65);
        def("v_oxygene", "C418 - Oxygène", "minecraft:music/game/oxygene", 141);

        // Креатив (знаходяться в music/game/creative/...)
        def("v_aria_math", "C418 - Aria Math", "minecraft:music/game/creative/aria_math", 310);
        def("v_biome_fest", "C418 - Biome Fest", "minecraft:music/game/creative/biome_fest", 378);
        def("v_blind_spots", "C418 - Blind Spots", "minecraft:music/game/creative/blind_spots", 332);
        def("v_haunt_muskie", "C418 - Haunt Muskie", "minecraft:music/game/creative/haunt_muskie", 361);
        def("v_dreiton", "C418 - Dreiton", "minecraft:music/game/creative/dreiton", 497);
        def("v_taswell", "C418 - Taswell", "minecraft:music/game/creative/taswell", 455);

        // Головне меню (знаходяться в music/menu/...)
        def("v_mutation", "C418 - Mutation", "minecraft:music/menu/mutation", 209);
        def("v_moog_city_2", "C418 - Moog City 2", "minecraft:music/menu/moog_city_2", 180);
        def("v_beginning_2", "C418 - Beginning 2", "minecraft:music/menu/beginning_2", 175);
        def("v_floating_trees", "C418 - Floating Trees", "minecraft:music/menu/floating_trees", 244);

        // The End та титри (знаходяться в music/game/end/...)
        def("v_the_end", "C418 - The End", "minecraft:music/game/end/the_end", 600);
        def("v_boss", "C418 - Boss (Dragon)", "minecraft:music/game/end/boss", 185);
        def("v_credits", "C418 - Alpha (Credits)", "minecraft:music/game/end/alpha", 603);
        def("v_endless", "Lena Raine - Endless", "minecraft:music/game/endless", 402);

        // Nether (знаходяться у відповідних біомах nether)
        def("v_chrysopoeia", "Lena Raine - Chrysopoeia", "minecraft:music/game/nether/crimson_forest/chrysopoeia", 303);
        def("v_rubedo", "Lena Raine - Rubedo", "minecraft:music/game/nether/nether_wastes/rubedo", 312);
        def("v_so_below", "Lena Raine - So Below", "minecraft:music/game/nether/soulsand_valley/so_below", 319);
        def("v_ballad_of_the_cats", "C418 - Ballad of the Cats", "minecraft:music/game/nether/ballad_of_the_cats", 275);
        def("v_dead_voxel", "C418 - Dead Voxel", "minecraft:music/game/nether/dead_voxel", 296);

        // Підводна музика (water)
        def("v_axolotl", "C418 - Axolotl", "minecraft:music/game/water/axolotl", 303);
        def("v_dragon_fish", "C418 - Dragon Fish", "minecraft:music/game/water/dragon_fish", 192);
        def("v_shuniji", "C418 - Shuniji", "minecraft:music/game/water/shuniji", 244);

        // Нові оновлення (1.18 - 1.21)
        def("v_infinite_amethyst", "Lena Raine - Infinite Amethyst", "minecraft:music/game/infinite_amethyst", 271);
        def("v_left_to_bloom", "Lena Raine - Left to Bloom", "minecraft:music/game/left_to_bloom", 342);
        def("v_one_more_day", "Lena Raine - One More Day", "minecraft:music/game/one_more_day", 278);
        def("v_stand_tall", "Lena Raine - Stand Tall", "minecraft:music/game/stand_tall", 308);
        def("v_wending", "Lena Raine - Wending", "minecraft:music/game/wending", 314);
        def("v_ancestry", "Lena Raine - Ancestry", "minecraft:music/game/ancestry", 343);
        def("v_echo_in_the_wind", "Aaron Cherof - Echo in the Wind", "minecraft:music/game/echo_in_the_wind", 304);
        def("v_a_familiar_room", "Aaron Cherof - A Familiar Room", "minecraft:music/game/a_familiar_room", 241);
        def("v_bromeliad", "Aaron Cherof - Bromeliad", "minecraft:music/game/bromeliad", 312);
        def("v_comforting_memories", "Kumi Tanioka - Comforting Memories", "minecraft:music/game/comforting_memories", 275);
        def("v_floating_dream", "Kumi Tanioka - Floating Dream", "minecraft:music/game/floating_dream", 205);
        def("v_an_ordinary_day", "Kumi Tanioka - An Ordinary Day", "minecraft:music/game/an_ordinary_day", 331);
        def("v_deeper", "Lena Raine - Deeper", "minecraft:music/game/deeper", 303);
        def("v_featherfall", "Aaron Cherof - Featherfall", "minecraft:music/game/featherfall", 344);
        def("v_komorebi", "Kumi Tanioka - Komorebi", "minecraft:music/game/komorebi", 287);
        def("v_pokopoko", "Kumi Tanioka - Poko Poko", "minecraft:music/game/pokopoko", 314);
        def("v_yakusoku", "Kumi Tanioka - Yakusoku", "minecraft:music/game/yakusoku", 271);

        def("v_crescent_dunes", "Aaron Cherof - Crescent Dunes", "minecraft:music/game/crescent_dunes", 248);
        def("v_eld_unknown", "Lena Raine - Eld Unknown", "minecraft:music/game/eld_unknown", 296);
        def("v_puzzlebox", "Aaron Cherof - Puzzlebox", "minecraft:music/game/puzzlebox", 298);
        def("v_watcher", "Aaron Cherof - Watcher", "minecraft:music/game/watcher", 312);
        def("v_aerie", "Lena Raine - Aerie", "minecraft:music/game/swamp/aerie", 316);
        def("v_firebugs", "Lena Raine - Firebugs", "minecraft:music/game/swamp/firebugs", 312);
        def("v_labyrinthine", "Lena Raine - Labyrinthine", "minecraft:music/game/swamp/labyrinthine", 324);
        def("v_concrete_halls", "C418 - Concrete Halls", "minecraft:music/game/nether/concrete_halls", 254);
        def("v_warmth", "C418 - Warmth", "minecraft:music/game/nether/warmth", 239);

        // Платівки (Music Discs, знаходяться в records/...)
        def("v_pigstep", "Lena Raine - Pigstep", "minecraft:records/pigstep", 149);
        def("v_otherside", "Lena Raine - Otherside", "minecraft:records/otherside", 195);
        def("v_creator", "Lena Raine - Creator", "minecraft:records/creator", 177);
        def("v_creator_music_box", "Lena Raine - Creator (Music Box)", "minecraft:records/creator_music_box", 74);
        def("v_precipice", "Aaron Cherof - Precipice", "minecraft:records/precipice", 299);
        def("v_relic", "Aaron Cherof - Relic", "minecraft:records/relic", 218);
        def("v_cat", "C418 - Cat", "minecraft:records/cat", 185);
        def("v_chirp", "C418 - Chirp", "minecraft:records/chirp", 185);
        def("v_mall", "C418 - Mall", "minecraft:records/mall", 197);
        def("v_stal", "C418 - Stal", "minecraft:records/stal", 150);
        def("v_strad", "C418 - Strad", "minecraft:records/strad", 188);
        def("v_blocks", "C418 - Blocks", "minecraft:records/blocks", 345);
        def("v_far", "C418 - Far", "minecraft:records/far", 174);
        def("v_mellohi", "C418 - Mellohi", "minecraft:records/mellohi", 96);
        def("v_ward", "C418 - Ward", "minecraft:records/ward", 251);
        def("v_13", "C418 - 13", "minecraft:records/13", 178);
        def("v_wait", "C418 - Wait", "minecraft:records/wait", 238);
        def("v_5", "Samuel Åberg - 5", "minecraft:records/5", 178);
        def("v_11", "C418 - 11", "minecraft:records/11", 71);
    }

    private static void def(String key, String displayName, String soundPath, int durationSeconds) {
        ALL_VANILLA_TRACKS.add(new VanillaTrackInfo(key, displayName, soundPath, durationSeconds));
    }

    public static List<TrackEntry> getDefaultVanillaTracks() {
        List<TrackEntry> list = new ArrayList<>();
        for (VanillaTrackInfo info : ALL_VANILLA_TRACKS) {
            list.add(new TrackEntry("custommusic:" + info.key(), info.displayName(), false, true, null));
        }
        return list;
    }

    public static VanillaTrackInfo findByKey(String key) {
        if (key == null) return null;
        if (key.contains("echo_in_the_canyon")) {
            key = key.replace("echo_in_the_canyon", "echo_in_the_wind");
        }
        for (VanillaTrackInfo info : ALL_VANILLA_TRACKS) {
            if (info.key().equals(key) || ("custommusic:" + info.key()).equals(key)) {
                return info;
            }
        }
        return null;
    }

    public static boolean isDisc(String trackId) {
        if (trackId == null) return false;
        VanillaTrackInfo info = findByKey(trackId);
        if (info != null) {
            return info.soundPath().contains("records/");
        }
        return trackId.contains("record") || trackId.contains("disc");
    }
}
