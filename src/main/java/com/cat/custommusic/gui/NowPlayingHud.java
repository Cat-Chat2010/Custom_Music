package com.cat.custommusic.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * HUD оверлей у правому верхньому кутку:
 * Показує плашку із затемненням та першими 10 символами назви пісні при її старті.
 */
public class NowPlayingHud {
    private static final int TOTAL_DISPLAY_TICKS = 140; // ~7 секунд
    private static String currentTrackShortName = null;
    private static int displayTicksRemaining = 0;

    public static void show(String trackName) {
        if (trackName == null) return;
        // Беремо перші 10 знаків пісні
        currentTrackShortName = trackName.length() > 10 ? trackName.substring(0, 10) : trackName;
        displayTicksRemaining = TOTAL_DISPLAY_TICKS;
    }

    public static void tick() {
        if (displayTicksRemaining > 0) {
            displayTicksRemaining--;
        }
    }

    public static void render(GuiGraphics graphics) {
        if (displayTicksRemaining <= 0 || currentTrackShortName == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) {
            return;
        }

        Font font = mc.font;
        Component text = Component.literal("🎵 " + currentTrackShortName).withStyle(ChatFormatting.WHITE);

        int textWidth = font.width(text);
        int paddingX = 8;
        int paddingY = 5;
        int boxWidth = textWidth + paddingX * 2;
        int boxHeight = 9 + paddingY * 2;

        int screenWidth = graphics.guiWidth();
        int boxX = screenWidth - boxWidth - 10;
        int boxY = 10;

        // Плавна прозорість: поява (перші 10 тіків) та згасання (останні 20 тіків)
        float alpha = 1.0f;
        int elapsed = TOTAL_DISPLAY_TICKS - displayTicksRemaining;
        if (elapsed < 10) {
            alpha = elapsed / 10.0f;
        } else if (displayTicksRemaining < 20) {
            alpha = displayTicksRemaining / 20.0f;
        }

        int bgAlpha = ((int) (alpha * 190) << 24) & 0xFF000000;
        int outlineAlpha = ((int) (alpha * 120) << 24) & 0xFF000000;
        int textAlpha = ((int) (alpha * 255) << 24) & 0xFF000000;

        // 1. Невелике затемнення фону
        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, bgAlpha | 0x111111);
        // 2. Акуратна тонка рамка
        graphics.renderOutline(boxX, boxY, boxWidth, boxHeight, outlineAlpha | 0x55FFFF);
        // 3. Текст назви (перші 10 знаків)
        graphics.drawString(font, text, boxX + paddingX, boxY + paddingY, textAlpha | 0xFFFFFF);
    }
}
