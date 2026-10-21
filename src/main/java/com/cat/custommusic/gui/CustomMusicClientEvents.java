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

            int buttonY = screenHeight - 28;
            for (var child : event.getScreen().children()) {
                if (child instanceof Button btn) {
                    if (btn.getY() >= screenHeight - 36 && btn.getWidth() >= 150) {
                        btn.setWidth(150);
                        btn.setX(screenWidth / 2 + 5);
                        buttonY = btn.getY();
                        break;
                    }
                }
            }

            Button customMusicButton = Button.builder(
                    Component.literal("🎵 Кастомна музика..."),
                    btn -> Minecraft.getInstance().setScreen(new CustomMusicScreen(soundOptionsScreen))
            ).bounds(screenWidth / 2 - 155, buttonY, 150, 20).build();

            event.addListener(customMusicButton);
        }
    }
}
