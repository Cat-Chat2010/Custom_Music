package com.cat.custommusic.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Клієнтські події інтерфейсу NeoForge.
 */
@EventBusSubscriber(modid = "custommusic", value = Dist.CLIENT)
public class CustomMusicClientEvents {

    /**
     * Додавання кнопки кастомної музики в екран SoundOptionsScreen.
     */
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof SoundOptionsScreen soundOptionsScreen) {
            int screenWidth = soundOptionsScreen.width;
            int screenHeight = soundOptionsScreen.height;

            // Зручні параметри кнопки (легко адаптувати під кастомну текстуру або іконку)
            int buttonWidth = 150;
            int buttonHeight = 20;
            int buttonX = screenWidth / 2 - (buttonWidth / 2);
            int buttonY = screenHeight - 52; // Над стандартною кнопкою "Готово"

            Button customMusicButton = Button.builder(
                    Component.literal("🎵 Кастомна музика..."),
                    btn -> Minecraft.getInstance().setScreen(new CustomMusicScreen(soundOptionsScreen))
            ).bounds(buttonX, buttonY, buttonWidth, buttonHeight).build();

            event.addListener(customMusicButton);
        }
    }
}
