package com.cat.custommusic.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/**
 * Реєстрація гарячої клавіші для відкриття плеєра (за замовчуванням Ctrl + P / Ctrl + Shift + P).
 * Клавішу можна вільно перепризначити в стандартних налаштуваннях керування Minecraft.
 */
public class CustomMusicKeyBindings {
    public static final String CATEGORY = "key.categories.custommusic";

    public static final KeyMapping OPEN_MUSIC_MENU_KEY = new KeyMapping(
            "key.custommusic.open_menu",
            KeyConflictContext.IN_GAME,
            KeyModifier.CONTROL,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            CATEGORY
    );

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MUSIC_MENU_KEY);
    }
}
