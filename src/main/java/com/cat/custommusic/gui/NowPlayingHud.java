package com.cat.custommusic.gui;

import com.cat.custommusic.audio.CustomMusicPlayer;
import com.cat.custommusic.config.TrackEntry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Міні-плеєр / HUD "Зараз грає" у правому верхньому кутку:
 * 1. Розташований впритул у правому верхньому кутку без відступів (x = screenWidth - BOX_WIDTH, y = 0).
 * 2. Гаряча клавіша Ctrl + O відкриває постійне віконце (закріплене, не закривається саме).
 * 3. Повторне натискання Ctrl + O закриває віконце.
 * 4. При зміні музики віконце плавно ховається (slide out) і виїжджає назад (slide in) з новою назвою.
 * 5. Під назвою:
 *    - Полоска прогресу відтворення музики.
 *    - Зліва над полоскою (під назвою): скільки часу пройшло у числах маленьким шрифтом (0:42).
 *    - Справа над полоскою: скільки залишилось із мінусом попереду (-2:18).
 *    - Зліва під полоскою тим же маленьким шрифтом: скільки усього триває пісня (3:00).
 */
public class NowPlayingHud {
    public enum AnimState {
        HIDDEN,
        SLIDING_IN,
        OPEN,
        SLIDING_OUT_FOR_NEXT,
        SLIDING_OUT_TO_CLOSE
    }

    // Компактний акуратний розмір міні-плеєра у правому верхньому кутку (зменшено по довжині)
    private static final int BOX_WIDTH = 96;
    private static final int BOX_HEIGHT = 34;

    private static final float SLIDE_SPEED = 0.12f; // ~8 тіків (0.4 сек) для плавної анімації

    private static boolean pinned = false;
    private static AnimState animState = AnimState.HIDDEN;
    private static float currentSlide = 0.0f; // 0.0f = повністю сховано, 1.0f = повністю на екрані

    private static String currentTrackDisplayText = null;
    private static String nextTrackDisplayText = null;
    private static int transientTicksRemaining = 0;

    /**
     * Перемикання режиму постійного міні-плеєра (Ctrl + O).
     */
    public static void togglePinned() {
        if (pinned) {
            pinned = false;
            transientTicksRemaining = 0;
            animState = AnimState.SLIDING_OUT_TO_CLOSE;
        } else {
            pinned = true;
            TrackEntry cur = CustomMusicPlayer.getInstance().getCurrentTrack();
            if (cur != null) {
                setDisplayText(cur.getDisplayName());
            } else {
                setDisplayText("Фонова музика");
            }
            animState = AnimState.SLIDING_IN;
        }
    }

    public static boolean isPinned() {
        return pinned;
    }

    /**
     * Обробка зміни треку (новий трек або ручне перемикання).
     */
    public static void onTrackChanged(String trackName) {
        if (trackName == null) return;

        if (pinned) {
            // Якщо віконце відкрите або виїжджає — ховаємо і дістаємо назад із новою піснею
            if (animState == AnimState.OPEN || animState == AnimState.SLIDING_IN) {
                nextTrackDisplayText = formatTitle(trackName);
                animState = AnimState.SLIDING_OUT_FOR_NEXT;
            } else if (animState == AnimState.SLIDING_OUT_FOR_NEXT) {
                nextTrackDisplayText = formatTitle(trackName);
            } else {
                setDisplayText(trackName);
                animState = AnimState.SLIDING_IN;
            }
        } else {
            // Звичайна поява на ~7 секунд при старті нового треку
            if (animState == AnimState.OPEN || animState == AnimState.SLIDING_IN) {
                // Якщо вже показується попередня пісня — плавно ховаємо і показуємо нову
                nextTrackDisplayText = formatTitle(trackName);
                animState = AnimState.SLIDING_OUT_FOR_NEXT;
            } else {
                setDisplayText(trackName);
                transientTicksRemaining = 140; // 7 секунд
                animState = AnimState.SLIDING_IN;
            }
        }
    }

    public static void onTrackStopped() {
        if (!pinned) {
            animState = AnimState.SLIDING_OUT_TO_CLOSE;
        }
    }

    // Для сумісності з попередніми викликами
    public static void show(String trackName) {
        onTrackChanged(trackName);
    }

    private static void setDisplayText(String rawName) {
        currentTrackDisplayText = formatTitle(rawName);
    }

    private static String formatTitle(String rawName) {
        if (rawName == null) return "Невідомий трек";
        if (rawName.length() > 18) {
            return rawName.substring(0, 18) + "...";
        }
        return rawName;
    }

    public static void tick() {
        switch (animState) {
            case SLIDING_IN -> {
                currentSlide += SLIDE_SPEED;
                if (currentSlide >= 1.0f) {
                    currentSlide = 1.0f;
                    animState = AnimState.OPEN;
                }
            }
            case SLIDING_OUT_FOR_NEXT -> {
                currentSlide -= SLIDE_SPEED;
                if (currentSlide <= 0.0f) {
                    currentSlide = 0.0f;
                    currentTrackDisplayText = nextTrackDisplayText;
                    nextTrackDisplayText = null;
                    animState = AnimState.SLIDING_IN;
                }
            }
            case SLIDING_OUT_TO_CLOSE -> {
                currentSlide -= SLIDE_SPEED;
                if (currentSlide <= 0.0f) {
                    currentSlide = 0.0f;
                    animState = AnimState.HIDDEN;
                }
            }
            case OPEN -> {
                if (!pinned) {
                    if (transientTicksRemaining > 0) {
                        transientTicksRemaining--;
                    } else {
                        animState = AnimState.SLIDING_OUT_TO_CLOSE;
                    }
                }
            }
            case HIDDEN -> {
                currentSlide = 0.0f;
            }
        }
    }

    public static void render(GuiGraphics graphics) {
        if (animState == AnimState.HIDDEN || currentTrackDisplayText == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) {
            return;
        }

        int screenWidth = graphics.guiWidth();
        float targetX = screenWidth - BOX_WIDTH;
        float hiddenX = screenWidth;

        // Плавна функція інтерполяції (Smoothstep ease)
        float ease = currentSlide * currentSlide * (3.0f - 2.0f * currentSlide);
        float currentX = hiddenX + (targetX - hiddenX) * ease;

        int boxX = Math.round(currentX);
        int boxY = 0; // Впритул до верхнього краю екрана

        // 1. Фон та рамка оверлею
        graphics.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + BOX_HEIGHT, 0xDD111111);
        graphics.renderOutline(boxX, boxY, BOX_WIDTH, BOX_HEIGHT, 0x55FFFFFF);

        Font font = mc.font;
        PoseStack pose = graphics.pose();

        // 2. Назва пісні (зверху, масштаб 0.65x)
        Component titleComp = Component.literal("🎵 " + currentTrackDisplayText);
        pose.pushPose();
        pose.translate(boxX + 4, boxY + 3, 0);
        pose.scale(0.65f, 0.65f, 1.0f);
        graphics.drawString(font, titleComp, 0, 0, 0xFFFFFFFF, false);
        pose.popPose();

        // 3. Дані таймера відтворення
        CustomMusicPlayer player = CustomMusicPlayer.getInstance();
        String elapsedStr = player.getFormattedElapsed();
        String remainingStr = player.getFormattedRemaining();
        String totalStr = player.getFormattedTotal();
        float progress = player.getTrackProgress();

        // 4. Текст над полоскою (але під назвою)
        // Зліва: скільки пройшло часу маленьким шрифтом (0.5x)
        pose.pushPose();
        pose.translate(boxX + 4, boxY + 12, 0);
        pose.scale(0.5f, 0.5f, 1.0f);
        graphics.drawString(font, elapsedStr, 0, 0, 0xFFE0E0E0, false);
        pose.popPose();

        // Справа: скільки залишилось із знаком мінус
        int remWidth = Math.round(font.width(remainingStr) * 0.5f);
        pose.pushPose();
        pose.translate(boxX + BOX_WIDTH - 4 - remWidth, boxY + 12, 0);
        pose.scale(0.5f, 0.5f, 1.0f);
        graphics.drawString(font, remainingStr, 0, 0, 0xFFE0E0E0, false);
        pose.popPose();

        // 5. Полоска прогресу відтворення
        int barX = boxX + 4;
        int barY = boxY + 20;
        int barWidth = BOX_WIDTH - 8;
        int barHeight = 2;

        // Темний фон смужки
        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);

        // Заповнена частина смужки (білий колір на запит користувача)
        int fillWidth = Math.round(barWidth * progress);
        if (fillWidth > 0) {
            graphics.fill(barX, barY, barX + fillWidth, barY + barHeight, 0xFFFFFFFF);
        }

        // 6. Текст під полоскою зліва: скільки усього триває пісня тим же шрифтом (0.5x)
        pose.pushPose();
        pose.translate(boxX + 4, boxY + 25, 0);
        pose.scale(0.5f, 0.5f, 1.0f);
        graphics.drawString(font, totalStr, 0, 0, 0xFFAAAAAA, false);
        pose.popPose();
    }
}
