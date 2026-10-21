package com.cat.custommusic.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * HUD плашка "Зараз грає" у правому верхньому кутку:
 * 1. Розташована в самому кутку екрана без відступів (x = screenWidth - BOX_WIDTH, y = 0).
 * 2. Текст у 2 рази менший (масштаб 0.65x).
 * 3. Відображає 10 символів назви пісні (+ "..." якщо назва довша).
 * 4. Плавна анімація: виїзд 0.5с, зупинка 7с, заїзд назад 0.5с.
 */
public class NowPlayingHud {
    private static final int SLIDE_IN_TICKS = 10;   // 0.5 секунди
    private static final int STAY_TICKS = 140;       // 7 секунд
    private static final int SLIDE_OUT_TICKS = 10;  // 0.5 секунди
    private static final int TOTAL_TICKS = SLIDE_IN_TICKS + STAY_TICKS + SLIDE_OUT_TICKS;

    // Фіксований компактний розмір плашки
    private static final int BOX_WIDTH = 96;
    private static final int BOX_HEIGHT = 14;

    private static String currentTrackDisplayText = null;
    private static int displayTicksRemaining = 0;

    public static void show(String trackName) {
        if (trackName == null) return;

        // 18 символів назви, якщо більше — додаємо "..."
        if (trackName.length() > 18) {
            currentTrackDisplayText = trackName.substring(0, 18) + "...";
        } else {
            currentTrackDisplayText = trackName;
        }

        displayTicksRemaining = TOTAL_TICKS;
    }

    public static void tick() {
        if (displayTicksRemaining > 0) {
            displayTicksRemaining--;
        }
    }

    public static void render(GuiGraphics graphics) {
        if (displayTicksRemaining <= 0 || currentTrackDisplayText == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) {
            return;
        }

        int screenWidth = graphics.guiWidth();
        // В самий кут екрана без жодного відступу
        float targetX = screenWidth - BOX_WIDTH;
        float hiddenX = screenWidth;
        float currentX;

        int elapsed = TOTAL_TICKS - displayTicksRemaining;

        if (elapsed < SLIDE_IN_TICKS) {
            // 1. Плавний виїзд справа (0.5 сек, ease-out)
            float progress = (float) elapsed / SLIDE_IN_TICKS;
            float ease = 1.0f - (1.0f - progress) * (1.0f - progress);
            currentX = hiddenX + (targetX - hiddenX) * ease;
        } else if (displayTicksRemaining > SLIDE_OUT_TICKS) {
            // 2. Стоїть на місці (7 секунд)
            currentX = targetX;
        } else {
            // 3. Плавний заїзд назад за край (0.5 сек, ease-in)
            float progress = (float) (SLIDE_OUT_TICKS - displayTicksRemaining) / SLIDE_OUT_TICKS;
            float ease = progress * progress;
            currentX = targetX + (hiddenX - targetX) * ease;
        }

        int boxX = Math.round(currentX);
        int boxY = 0; // Самий верхній край екрана

        // Затемнений фон плашки (напівпрозорий темний)
        graphics.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + BOX_HEIGHT, 0xCC111111);
        // Тонка контурна рамка
        graphics.renderOutline(boxX, boxY, BOX_WIDTH, BOX_HEIGHT, 0x55FFFFFF);

        // Текст у 2 рази менший (масштаб ~0.65x)
        Font font = mc.font;
        Component text = Component.literal("🎵 " + currentTrackDisplayText);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(boxX + 3, boxY + 4, 0);
        pose.scale(0.65f, 0.65f, 1.0f);

        // Білий текст
        graphics.drawString(font, text, 0, 0, 0xFFFFFFFF, false);
        pose.popPose();
    }
}
