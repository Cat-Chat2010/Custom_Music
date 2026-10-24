package com.cat.custommusic.gui;

import com.cat.custommusic.CustomMusicMod;
import com.cat.custommusic.audio.CustomMusicPlayer;
import com.cat.custommusic.config.MusicConfig;
import com.cat.custommusic.config.PlaybackMode;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Головний екран налаштувань та керування кастомною музикою.
 */
public class CustomMusicScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomMusicScreen.class);

    private final Screen parentScreen;
    private TrackListWidget trackList;
    private Button modeButton;
    private Button addMusicButton;
    private DelaySlider minDelaySlider;
    private DelaySlider maxDelaySlider;
    private Component statusComponent = null;

    public CustomMusicScreen(Screen parentScreen) {
        super(Component.translatable("gui.custommusic.title"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        // Гарантуємо, що відтворення звуку активне в налаштуваннях
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().resume();
        }

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
                Component.translatable("gui.custommusic.add_music"),
                b -> openAddMusicAction()
        ).bounds(centerRight, topY, 150, 20).build();
        this.addRenderableWidget(addMusicButton);

        // 2. Слайдери затримок між треками (0 - 600 секунд)
        int slidersY = topY + 24;
        this.minDelaySlider = new DelaySlider(
                centerLeft, slidersY, 150, 20,
                "gui.custommusic.min_delay",
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
                "gui.custommusic.max_delay",
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
                Component.translatable("gui.custommusic.next_track"),
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
     * Відкриває нативне системне вікно вибору файлів (Windows OpenFileDialog / провідник).
     * Обрана музика БЕЗПЕЧНО копіюється в папку гри (оригінали користувача залишаються недоторканими!).
     */
    private void openAddMusicAction() {
        this.statusComponent = Component.translatable("gui.custommusic.status.choose_files").withStyle(ChatFormatting.YELLOW);

        CompletableFuture.runAsync(() -> {
            List<File> selectedFiles = new ArrayList<>();
            String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            String dialogTitle = I18n.get("gui.custommusic.dialog.title");
            String filterAudio = I18n.get("gui.custommusic.dialog.filter_audio");
            String filterAll = I18n.get("gui.custommusic.dialog.filter_all");

            // 1. Для Windows запускаємо нативний OpenFileDialog окремим фоновим процесом
            // Це відкриває справжній провідник вибору файлів перед екраном гри
            if (osName.contains("win")) {
                try {
                    String escapedTitle = dialogTitle.replace("'", "''");
                    String escapedAudio = filterAudio.replace("'", "''");
                    String escapedAll = filterAll.replace("'", "''");

                    String psCommand = "Add-Type -AssemblyName System.Windows.Forms; " +
                            "$d = New-Object System.Windows.Forms.OpenFileDialog; " +
                            "$d.Multiselect = $true; " +
                            "$d.Title = '" + escapedTitle + "'; " +
                            "$d.Filter = '" + escapedAudio + " (*.mp3;*.ogg)|*.mp3;*.ogg|" + escapedAll + " (*.*)|*.*'; " +
                            "if ($d.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) { $d.FileNames }";

                    ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command", psCommand);
                    Process process = pb.start();

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String trimmed = line.trim();
                            if (!trimmed.isEmpty()) {
                                File f = new File(trimmed);
                                if (f.isFile() && f.exists()) {
                                    selectedFiles.add(f);
                                }
                            }
                        }
                    }
                    process.waitFor();
                } catch (Throwable t) {
                    LOGGER.warn("[CustomMusic] Помилка PowerShell діалогу: ", t);
                }
            }

            // 2. Для Mac/Linux або якщо PowerShell не відкрився — нативний AWT FileDialog
            if (selectedFiles.isEmpty() && !osName.contains("win")) {
                try {
                    java.awt.Frame dummyFrame = new java.awt.Frame();
                    dummyFrame.setUndecorated(true);
                    dummyFrame.setLocationRelativeTo(null);
                    dummyFrame.setAlwaysOnTop(true);
                    dummyFrame.setVisible(true);
                    dummyFrame.toFront();

                    java.awt.FileDialog dialog = new java.awt.FileDialog(dummyFrame, dialogTitle, java.awt.FileDialog.LOAD);
                    dialog.setMultipleMode(true);
                    dialog.setAlwaysOnTop(true);
                    dialog.toFront();
                    dialog.setVisible(true);

                    File[] files = dialog.getFiles();
                    dummyFrame.dispose();
                    if (files != null) {
                        Collections.addAll(selectedFiles, files);
                    }
                } catch (Throwable ignored) {
                }
            }

            // 3. Обробка обраних файлів
            if (!selectedFiles.isEmpty()) {
                Path rootDir = CustomMusicMod.getInstance().getDirectoryManager().getRootDir();
                int copied = 0;
                for (File srcFile : selectedFiles) {
                    String lowerName = srcFile.getName().toLowerCase(Locale.ROOT);
                    if (lowerName.endsWith(".mp3") || lowerName.endsWith(".ogg")) {
                        Path dest = rootDir.resolve(srcFile.getName());
                        try {
                            // БЕЗПЕЧНЕ КОПІЮВАННЯ: оригінальні файли залишаються на ПК користувача в тому самому місці й форматі!
                            Files.copy(srcFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                            copied++;
                        } catch (Exception ex) {
                            LOGGER.error("[CustomMusic] Не вдалося скопіювати файл: " + srcFile.getName(), ex);
                        }
                    }
                }

                final int count = copied;
                if (this.minecraft != null) {
                    this.minecraft.execute(() -> {
                        if (count > 0) {
                            this.statusComponent = Component.translatable("gui.custommusic.status.copied_ready", count).withStyle(ChatFormatting.GREEN);
                            CustomMusicMod.getInstance().refreshTracks();
                            refreshList();
                            // Перезавантажуємо ресурси, щоб Minecraft зареєстрував нові треки
                            this.minecraft.reloadResourcePacks();
                        } else {
                            this.statusComponent = Component.translatable("gui.custommusic.status.no_new_tracks").withStyle(ChatFormatting.YELLOW);
                        }
                    });
                }
            } else {
                if (this.minecraft != null) {
                    this.minecraft.execute(() -> this.statusComponent = null);
                }
            }
        });
    }

    public void refreshList() {
        if (trackList != null) {
            trackList.refreshList();
        }
    }

    private Component getModeButtonText(PlaybackMode mode) {
        return Component.translatable("gui.custommusic.mode", mode.getComponent());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Забезпечуємо безперервне звучання музики в налаштуваннях
        if (this.minecraft != null && CustomMusicPlayer.getInstance().isPlaying()) {
            this.minecraft.getSoundManager().resume();
        }

        super.render(graphics, mouseX, mouseY, partialTick);

        // Заголовок
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);

        // Статусне повідомлення (помилки/прогрес конвертації)
        if (statusComponent != null) {
            graphics.drawCenteredString(this.font, statusComponent, this.width / 2, 22, 0xFFFFFF);
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }
}
