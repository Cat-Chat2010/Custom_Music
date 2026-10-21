package com.cat.custommusic.gui;

import com.cat.custommusic.CustomMusicMod;
import com.cat.custommusic.audio.CustomMusicPlayer;
import com.cat.custommusic.audio.FfmpegConverter;
import com.cat.custommusic.config.MusicConfig;
import com.cat.custommusic.config.PlaybackMode;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;

/**
 * Головний екран налаштувань та керування кастомною музикою.
 */
public class CustomMusicScreen extends Screen {
    private final Screen parentScreen;
    private TrackListWidget trackList;
    private Button modeButton;
    private Button addMusicButton;
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

        // 1. Верхній блок керування: Режим відтворення, кнопка додавання файлів та кнопка імпорту
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

        // Кнопка відкриття провідника / вибору файлів
        this.addMusicButton = Button.builder(
                Component.literal("📁 Додати музику"),
                b -> openAddMusicAction()
        ).bounds(centerRight, topY, 96, 20).build();
        this.addRenderableWidget(addMusicButton);

        // Кнопка конвертації та імпорту
        this.importButton = Button.builder(
                Component.literal("🔄"),
                b -> startImportProcess()
        ).bounds(centerRight + 100, topY, 50, 20).build();
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

    /**
     * Відкриває нативне системне вікно вибору аудіофайлів для додавання музики.
     * Обрані файли безпечно копіюються в папку гри (оригінали користувача НЕ видаляються!).
     */
    private void openAddMusicAction() {
        this.statusMessage = "§eОберіть аудіофайли у системному вікні...";

        CompletableFuture.runAsync(() -> {
            try {
                // Створюємо фрейм поверх усіх вікон, щоб діалог вибору з'явився поверх гри
                java.awt.Frame dummyFrame = new java.awt.Frame();
                dummyFrame.setUndecorated(true);
                dummyFrame.setLocationRelativeTo(null);
                dummyFrame.setAlwaysOnTop(true);
                dummyFrame.setVisible(true);
                dummyFrame.toFront();

                java.awt.FileDialog dialog = new java.awt.FileDialog(dummyFrame, "Оберіть аудіофайли для Minecraft", java.awt.FileDialog.LOAD);
                dialog.setMultipleMode(true);
                dialog.setAlwaysOnTop(true);
                dialog.toFront();
                dialog.requestFocus();
                dialog.setVisible(true);

                File[] selectedFiles = dialog.getFiles();
                dummyFrame.dispose();

                if (selectedFiles != null && selectedFiles.length > 0) {
                    Path importDir = CustomMusicMod.getInstance().getDirectoryManager().getImportDir();
                    int copied = 0;
                    for (File srcFile : selectedFiles) {
                        Path dest = importDir.resolve(srcFile.getName());
                        // БЕЗПЕЧНЕ КОПІЮВАННЯ: оригінальні файли залишаються на комп'ютері недоторканими!
                        Files.copy(srcFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                        copied++;
                    }
                    final int count = copied;
                    if (this.minecraft != null) {
                        this.minecraft.execute(() -> {
                            this.statusMessage = "§aСкопійовано " + count + " треків! Конвертуємо...";
                            startImportProcess();
                        });
                    }
                } else {
                    if (this.minecraft != null) {
                        this.minecraft.execute(() -> this.statusMessage = "");
                    }
                }
            } catch (Throwable t) {
                // Fallback: якщо графічний діалог не підтримується системою, відкриваємо папку
                Path importDir = CustomMusicMod.getInstance().getDirectoryManager().getImportDir();
                Util.getPlatform().openPath(importDir);
                if (this.minecraft != null) {
                    this.minecraft.execute(() -> this.statusMessage = "§eВідкрито папку import. Додайте ваші треки сюди.");
                }
            }
        });
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
                            this.statusMessage = count > 0 ? "§aСконвертовано " + count + " треків! Оновлюємо ресурси..." : "§eНових файлів не знайдено.";
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
