package com.cat.custommusic.gui;

import com.cat.custommusic.CustomMusicMod;
import com.cat.custommusic.audio.CustomMusicPlayer;
import com.cat.custommusic.audio.FfmpegConverter;
import com.cat.custommusic.config.MusicConfig;
import com.cat.custommusic.config.PlaybackMode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Головний екран налаштувань та керування кастомною музикою.
 */
public class CustomMusicScreen extends Screen {
    private final Screen parentScreen;
    private TrackListWidget trackList;
    private Button modeButton;
    private Button importButton;
    private DelaySlider minDelaySlider;
    private DelaySlider maxDelaySlider;
    private String statusMessage = "";

    public CustomMusicScreen(Screen parentScreen) {
        super(Component.literal("Налаштування фонової музики"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        MusicConfig config = CustomMusicMod.getInstance().getConfig();

        // 1. Верхній блок керування: Режим відтворення та кнопка імпорту
        int topY = 32;
        int centerLeft = this.width / 2 - 155;
        int centerRight = this.width / 2 + 5;

        this.modeButton = Button.builder(
                getModeButtonText(config.getPlaybackMode()),
                b -> {
                    PlaybackMode next = config.getPlaybackMode().next();
                    config.setPlaybackMode(next);
                    b.setMessage(getModeButtonText(next));
                }
        ).bounds(centerLeft, topY, 150, 20).build();
        this.addRenderableWidget(modeButton);

        this.importButton = Button.builder(
                Component.literal("Імпортувати треки"),
                b -> startImportProcess()
        ).bounds(centerRight, topY, 150, 20).build();
        this.addRenderableWidget(importButton);

        // 2. Слайдери затримок між треками (0 - 600 секунд)
        int slidersY = topY + 24;
        this.minDelaySlider = new DelaySlider(
                centerLeft, slidersY, 150, 20,
                "Мін. затримка",
                config.getMinDelaySeconds(), 0, 600,
                val -> {
                    config.setMinDelaySeconds(val);
                    if (maxDelaySlider != null && maxDelaySlider.getSeconds() < val) {
                        maxDelaySlider.setSeconds(val);
                    }
                }
        );
        this.addRenderableWidget(minDelaySlider);

        this.maxDelaySlider = new DelaySlider(
                centerRight, slidersY, 150, 20,
                "Макс. затримка",
                config.getMaxDelaySeconds(), 0, 600,
                val -> {
                    config.setMaxDelaySeconds(val);
                    if (minDelaySlider != null && minDelaySlider.getSeconds() > val) {
                        minDelaySlider.setSeconds(val);
                    }
                }
        );
        this.addRenderableWidget(maxDelaySlider);

        // 3. Прокручуваний список треків
        int listTop = slidersY + 26;
        int listBottom = this.height - 36;
        this.trackList = new TrackListWidget(this.minecraft, this.width, listBottom - listTop, listTop, 24, this);
        this.addRenderableWidget(trackList);

        // 4. Нижні кнопки: Наступний трек та Готово
        int bottomY = this.height - 28;
        this.addRenderableWidget(Button.builder(
                Component.literal("Наступний трек ⏭"),
                b -> CustomMusicPlayer.getInstance().playNextTrack()
        ).bounds(this.width / 2 - 155, bottomY, 150, 20).build());

        this.addRenderableWidget(Button.builder(
                CommonComponents.GUI_DONE,
                b -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(this.parentScreen);
                    }
                }
        ).bounds(this.width / 2 + 5, bottomY, 150, 20).build());
    }

    private void startImportProcess() {
        FfmpegConverter converter = CustomMusicMod.getInstance().getFfmpegConverter();
        if (!converter.isAvailable()) {
            this.statusMessage = "§cFFmpeg не знайдено в PATH чи custom_music/ffmpeg/!";
            return;
        }

        this.importButton.active = false;
        this.statusMessage = "§eКонвертація файлів із папки import/...";

        converter.convertAllImportsAsync(msg -> this.statusMessage = "§7" + msg)
                .thenAccept(count -> {
                    if (this.minecraft != null) {
                        this.minecraft.execute(() -> {
                            this.importButton.active = true;
                            this.statusMessage = count > 0 ? "§aСконвертовано " + count + " треків! Оновлюємо звуки..." : "§eНових файлів не знайдено.";
                            CustomMusicMod.getInstance().refreshTracks();
                            refreshList();
                            // Перезавантажуємо ресурси, щоб Minecraft підтягнув нові .ogg треки
                            if (count > 0 && this.minecraft != null) {
                                this.minecraft.reloadResourcePacks();
                            }
                        });
                    }
                });
    }

    public void refreshList() {
        if (trackList != null) {
            trackList.refreshList();
        }
    }

    private Component getModeButtonText(PlaybackMode mode) {
        return Component.literal("Режим: " + mode.getDisplayName());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        // Заголовок
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);

        // Статусне повідомлення (помилки/прогрес конвертації)
        if (!statusMessage.isEmpty()) {
            graphics.drawCenteredString(this.font, statusMessage, this.width / 2, 22, 0xFFFFFF);
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }
}
