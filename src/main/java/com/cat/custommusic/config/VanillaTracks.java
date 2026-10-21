package com.cat.custommusic.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Довідковий список стандартних ванільних фонових треків Minecraft.
 */
public class VanillaTracks {

    public static List<TrackEntry> getDefaultVanillaTracks() {
        List<TrackEntry> list = new ArrayList<>();
        add(list, "minecraft:music.game", "Minecraft - Overworld (General)");
        add(list, "minecraft:music.creative", "Minecraft - Creative");
        add(list, "minecraft:music.menu", "Minecraft - Main Menu");
        add(list, "minecraft:music.under_water", "Minecraft - Underwater");
        add(list, "minecraft:music.nether.nether_wastes", "Nether - Wastes");
        add(list, "minecraft:music.nether.crimson_forest", "Nether - Crimson Forest");
        add(list, "minecraft:music.nether.warped_forest", "Nether - Warped Forest");
        add(list, "minecraft:music.nether.soul_sand_valley", "Nether - Soul Sand Valley");
        add(list, "minecraft:music.nether.basalt_deltas", "Nether - Basalt Deltas");
        add(list, "minecraft:music.end", "Minecraft - The End");
        add(list, "minecraft:music.dragon", "Minecraft - Ender Dragon");
        add(list, "minecraft:music.credits", "Minecraft - Credits");
        return list;
    }

    private static void add(List<TrackEntry> list, String soundId, String name) {
        list.add(new TrackEntry(soundId, name, false, true, null));
    }
}
