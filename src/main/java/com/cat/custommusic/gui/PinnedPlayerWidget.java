package com.cat.custommusic.gui;

import com.cat.custommusic.audio.CustomMusicPlayer;
import com.cat.custommusic.config.TrackEntry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * Закріплений міні-плеєр у меню Ctrl + P:
 * - Розташований між налаштуваннями та списком треків.
 * - Повністю повторює темний стиль HUD-плашки.
 * - Плавне перетягування смужки (Seeking спрацьовує безпечно при відпусканні кнопки миші).
 * - Кнопки керування: ⏮, -5s, ▶/⏸, +5s, ⏭ (без зайвого квадрата Стоп).
 * - Значки статусу відтворення (▶) та паузи (⏸) на 100% узгоджені з HUD.
 */
public class PinnedPlayerWidget extends AbstractWidget {

    private boolean isDraggingBar = false;
    private float draggedProgress = 0.0f;

    public PinnedPlayerWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        CustomMusicPlayer player = CustomMusicPlayer.getInstance();
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        PoseStack pose = graphics.pose();

        int bx = getX();
        int by = getY();
        int w = getWidth();
        int h = getHeight();

        // 1. Фон та контур (ідентично до HUD)
        graphics.fill(bx, by, bx + w, by + h, 0xDD111111);
        graphics.renderOutline(bx, by, w, h, 0x55FFFFFF);

        // 2. Назва поточного треку (зверху, масштаб 0.7x)
        TrackEntry cur = player.getCurrentTrack();
        String rawTitle = (cur != null) ? cur.getDisplayName() : "Фонова музика не грає";
        String displayTitle = "🎵 " + (rawTitle.length() > 28 ? rawTitle.substring(0, 28) + "..." : rawTitle);

        pose.pushPose();
        pose.translate(bx + 8, by + 4, 0);
        pose.scale(0.7f, 0.7f, 1.0f);
        graphics.drawString(font, displayTitle, 0, 0, 0xFFFFFFFF, false);
        pose.popPose();

        // 3. Розрахунок часу та прогресу
        boolean isPlaying = player.isPlaying() && !player.isPaused();
        boolean isPaused = player.isPaused();
        int totalSec = player.getTrackTotalSeconds();

        float progress = isDraggingBar ? draggedProgress : player.getTrackProgress();
        int elapsedSec = Math.round(progress * totalSec);
        int remSec = Math.max(0, totalSec - elapsedSec);

        String elapsedStr = CustomMusicPlayer.formatDuration(elapsedSec);
        String remainingStr = "-" + CustomMusicPlayer.formatDuration(remSec);
        String totalStr = CustomMusicPlayer.formatDuration(totalSec);

        // Пройдений час зліва над смужкою (0.55x)
        pose.pushPose();
        pose.translate(bx + 8, by + 14, 0);
        pose.scale(0.55f, 0.55f, 1.0f);
        graphics.drawString(font, elapsedStr, 0, 0, 0xFFE0E0E0, false);
        pose.popPose();

        // Час до кінця справа над смужкою (0.55x)
        int remW = Math.round(font.width(remainingStr) * 0.55f);
        pose.pushPose();
        pose.translate(bx + w - 8 - remW, by + 14, 0);
        pose.scale(0.55f, 0.55f, 1.0f);
        graphics.drawString(font, remainingStr, 0, 0, 0xFFE0E0E0, false);
        pose.popPose();

        // 4. Інтерактивна смужка прогресу
        int barX = bx + 8;
        int barY = by + 23;
        int barW = w - 16;
        int barH = 3;

        boolean isHoveringBar = mouseX >= barX && mouseX <= barX + barW && mouseY >= barY - 3 && mouseY <= barY + barH + 3;

        // Фон смужки
        graphics.fill(barX, barY, barX + barW, barY + barH, isHoveringBar || isDraggingBar ? 0xFF444444 : 0xFF282828);

        // Заповнена частина білим кольором
        int fillW = Math.round(barW * progress);
        if (fillW > 0) {
            graphics.fill(barX, barY, barX + fillW, barY + barH, 0xFFFFFFFF);
        }

        // Повзунок-індикатор на смужці
        int scrubberX = barX + fillW;
        graphics.fill(scrubberX - 1, barY - 1, scrubberX + 2, barY + barH + 1, isHoveringBar || isDraggingBar ? 0xFFFFDD44 : 0xFFFFFFFF);

        // 5. Нижня частина: Загальна тривалість зліва
        pose.pushPose();
        pose.translate(bx + 8, by + 34, 0);
        pose.scale(0.55f, 0.55f, 1.0f);
        graphics.drawString(font, totalStr, 0, 0, 0xFFAAAAAA, false);
        pose.popPose();

        // 6. Кнопки керування (5 кнопок: ⏮, -5s, ⏸/▶, +5s, ⏭ без квадрата Стоп)
        int btnY = by + 29;
        int btnH = 15;

        // Значок кнопки відповідає поточному стану: 2 полоски (⏸) коли грає, трикутник (▶) коли пауза
        String playPauseIcon = isPlaying ? "⏸" : "▶";

        renderButton(graphics, font, mouseX, mouseY, bx + 42, btnY, 26, btnH, "⏮", 0.7f);
        renderButton(graphics, font, mouseX, mouseY, bx + 72, btnY, 30, btnH, "-5s", 0.6f);
        renderButton(graphics, font, mouseX, mouseY, bx + 106, btnY, 32, btnH, playPauseIcon, 0.75f);
        renderButton(graphics, font, mouseX, mouseY, bx + 142, btnY, 30, btnH, "+5s", 0.6f);
        renderButton(graphics, font, mouseX, mouseY, bx + 176, btnY, 26, btnH, "⏭", 0.7f);
    }

    private void renderButton(GuiGraphics graphics, Font font, int mouseX, int mouseY, int x, int y, int w, int h, String label, float scale) {
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        graphics.fill(x, y, x + w, y + h, hover ? 0x88444444 : 0x55222222);
        graphics.renderOutline(x, y, w, h, hover ? 0xFFFFFFFF : 0x44FFFFFF);

        int textW = Math.round(font.width(label) * scale);
        int textH = Math.round(font.lineHeight * scale);
        float tx = x + (w - textW) / 2.0f;
        float ty = y + (h - textH) / 2.0f;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(tx, ty, 0);
        pose.scale(scale, scale, 1.0f);
        graphics.drawString(font, label, 0, 0, hover ? 0xFFFFFF55 : 0xFFFFFFFF, false);
        pose.popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible || button != 0) {
            return false;
        }

        int bx = getX();
        int by = getY();
        int w = getWidth();

        // 1. Натискання на смужку прогресу
        int barX = bx + 8;
        int barY = by + 23;
        int barW = w - 16;
        int barH = 3;

        if (mouseX >= barX && mouseX <= barX + barW && mouseY >= barY - 4 && mouseY <= barY + barH + 4) {
            this.isDraggingBar = true;
            this.draggedProgress = Math.max(0.0f, Math.min(1.0f, (float) (mouseX - barX) / (float) barW));
            return true;
        }

        // 2. Кнопки керування
        int btnY = by + 29;
        int btnH = 15;
        CustomMusicPlayer player = CustomMusicPlayer.getInstance();

        if (isInside(mouseX, mouseY, bx + 42, btnY, 26, btnH)) { // ⏮ Prev
            player.playPreviousTrack();
            playClickSound();
            return true;
        }
        if (isInside(mouseX, mouseY, bx + 72, btnY, 30, btnH)) { // -5s
            player.seekRelative(-5);
            playClickSound();
            return true;
        }
        if (isInside(mouseX, mouseY, bx + 106, btnY, 32, btnH)) { // ▶ / ⏸ Play / Pause
            player.togglePlayPause();
            playClickSound();
            return true;
        }
        if (isInside(mouseX, mouseY, bx + 142, btnY, 30, btnH)) { // +5s
            player.seekRelative(5);
            playClickSound();
            return true;
        }
        if (isInside(mouseX, mouseY, bx + 176, btnY, 26, btnH)) { // ⏭ Next
            player.playNextTrack();
            playClickSound();
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isDraggingBar) {
            int barX = getX() + 8;
            int barW = getWidth() - 16;
            this.draggedProgress = Math.max(0.0f, Math.min(1.0f, (float) (mouseX - barX) / (float) barW));
            // Не викликаємо seekTo під час перетягування, щоб не перевантажувати аудіорушій
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.isDraggingBar) {
            this.isDraggingBar = false;
            CustomMusicPlayer player = CustomMusicPlayer.getInstance();
            int totalSec = player.getTrackTotalSeconds();
            int targetSec = Math.round(this.draggedProgress * totalSec);
            player.seekTo(targetSec);
            playClickSound();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isInside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
