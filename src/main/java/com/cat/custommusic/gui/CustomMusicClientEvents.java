package com.cat.custommusic.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Клієнтські події інтерфейсу NeoForge.
 */
@EventBusSubscriber(modid = "custommusic", value = Dist.CLIENT)
public class CustomMusicClientEvents {

    /**
     * Додавання кнопки кастомної музики в екран SoundOptionsScreen
     * у порожній слот праворуч від кнопки "Диктор, голос".
     */
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof SoundOptionsScreen soundOptionsScreen) {
            int screenWidth = soundOptionsScreen.width;
            int screenHeight = soundOptionsScreen.height;

            int leftColX = screenWidth / 2 - 155;
            int rightColX = screenWidth / 2 + 5;
            int targetX = rightColX;
            int targetY = -1;

            // Відновлюємо кнопку "Готово" до її оригінального розміру та розташування по центру
            for (var child : event.getScreen().children()) {
                if (child instanceof Button btn) {
                    if (btn.getY() >= screenHeight - 36 && btn.getWidth() >= 140) {
                        btn.setWidth(200);
                        btn.setX(screenWidth / 2 - 100);
                        break;
                    }
                }
            }

            // Шукаємо рядок у сітці, де лівий віджет є, а правого немає (порожній слот навпроти "Диктор")
            for (var child : event.getScreen().children()) {
                if (child instanceof AbstractWidget widget) {
                    if (widget.getX() == leftColX && widget.getWidth() == 150) {
                        boolean hasRightPartner = false;
                        for (var other : event.getScreen().children()) {
                            if (other instanceof AbstractWidget otherWidget) {
                                if (otherWidget != widget && otherWidget.getX() == rightColX && otherWidget.getY() == widget.getY()) {
                                    hasRightPartner = true;
                                    break;
                                }
                            }
                        }
                        if (!hasRightPartner) {
                            targetY = widget.getY();
                            break;
                        }
                    }
                }
            }

            // Якщо раптом не визначився Y — fallback
            if (targetY == -1) {
                targetY = screenHeight / 6 + 120;
            }

            Button customMusicButton = Button.builder(
                    Component.literal("🎵 Кастомна музика..."),
                    btn -> Minecraft.getInstance().setScreen(new CustomMusicScreen(soundOptionsScreen))
            ).bounds(targetX, targetY, 150, 20).build();

            event.addListener(customMusicButton);
        }
    }

    /**
     * Відображення HUD оверлею "Зараз грає" у правому верхньому кутку екрана.
     */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        NowPlayingHud.render(event.getGuiGraphics());
    }

    /**
     * Обробка таймера HUD та гарячої клавіші виклику меню музики.
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        NowPlayingHud.tick();
        com.cat.custommusic.audio.CustomMusicPlayer.getInstance().tick();

        Minecraft mc = Minecraft.getInstance();
        while (CustomMusicKeyBindings.OPEN_MUSIC_MENU_KEY.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new CustomMusicScreen(null));
            }
        }
    }
}
