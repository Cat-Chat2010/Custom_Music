package com.cat.custommusic.gui;

import com.cat.custommusic.CustomMusicMod;
import com.cat.custommusic.audio.CustomMusicPlayer;
import com.cat.custommusic.config.Album;
import com.cat.custommusic.config.MusicConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
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
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Головний екран налаштувань та керування кастомною музикою:
 * 1. Вкладка "Альбоми":
 *    - Створення, видалення та вибір альбомів (перманентний альбом "Усі пісні").
 *    - Інтерактивний плеєр (таймлайн, перемотка, пауза/плей, перемикання).
 *    - Пошук пісень за назвою.
 *    - Упорядкування (За назвою, За довжиною, За типом, Своє) та перемішування.
 *    - Перетягування Drag-and-Drop (вмикає режим "Своє").
 *    - Додавання та прибирання пісень з альбому.
 * 2. Вкладка "Налаштування музики":
 *    - Затримка тиші між піснями.
 *    - Гучність за категоріями (кастомна, ванільна, платівки).
 *    - Додавання та видалення музичних файлів.
 *    - Редагування гучності окремих пісень (при досягненні 100% пісня зникає зі списку).
 */
public class CustomMusicScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomMusicScreen.class);

    public enum Tab {
        ALBUMS,
        SETTINGS
    }

    private static Tab currentTab = Tab.ALBUMS;

    private final Screen parentScreen;

    // Вкладка "Альбоми"
    private TrackListWidget trackList;
    private PinnedPlayerWidget pinnedPlayer;
    private EditBox searchBox;
    private Button sortButton;
    private Button shuffleButton;
    private Button prevAlbumButton;
    private Button nextAlbumButton;
    private Button currentAlbumButton;
    private Button newAlbumButton;
    private Button deleteAlbumButton;
    private Button addSongToAlbumButton;

    // Вкладка "Налаштування"
    private DelaySlider minDelaySlider;
    private DelaySlider maxDelaySlider;
    private VolumeSlider customVolumeSlider;
    private VolumeSlider vanillaVolumeSlider;
    private VolumeSlider discVolumeSlider;
    private Button addMusicFileButton;
    private Button deleteMusicFileButton;
    private Button customizeTrackVolumeButton;
    private CustomVolumeListWidget customVolumeListWidget;

    private Component statusComponent = null;

    public CustomMusicScreen(Screen parentScreen) {
        super(Component.translatable("gui.custommusic.title"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        if (this.minecraft != null && !CustomMusicPlayer.getInstance().isPaused()) {
            this.minecraft.getSoundManager().resume();
        }

        MusicConfig config = CustomMusicMod.getInstance().getConfig();
        if (config == null) return;

        // 1. Верхня панель вкладок: [ 💿 Альбоми ] та [ ⚙ Налаштування ]
        int tabBtnWidth = 145;
        int tabBtnY = 6;
        int tabBtnLeft = this.width / 2 - tabBtnWidth - 5;
        int tabBtnRight = this.width / 2 + 5;

        Component albumsTabTitle = Component.translatable("gui.custommusic.tab.albums");
        if (currentTab == Tab.ALBUMS) {
            albumsTabTitle = Component.literal("▶ ").append(albumsTabTitle).append(" ◀").withStyle(ChatFormatting.YELLOW);
        }
        Button albumsTabBtn = Button.builder(
                albumsTabTitle,
                b -> switchTab(Tab.ALBUMS)
        ).bounds(tabBtnLeft, tabBtnY, tabBtnWidth, 20).build();
        albumsTabBtn.active = (currentTab != Tab.ALBUMS);
        this.addRenderableWidget(albumsTabBtn);

        Component settingsTabTitle = Component.translatable("gui.custommusic.tab.settings");
        if (currentTab == Tab.SETTINGS) {
            settingsTabTitle = Component.literal("▶ ").append(settingsTabTitle).append(" ◀").withStyle(ChatFormatting.YELLOW);
        }
        Button settingsTabBtn = Button.builder(
                settingsTabTitle,
                b -> switchTab(Tab.SETTINGS)
        ).bounds(tabBtnRight, tabBtnY, tabBtnWidth, 20).build();
        settingsTabBtn.active = (currentTab != Tab.SETTINGS);
        this.addRenderableWidget(settingsTabBtn);

        if (currentTab == Tab.ALBUMS) {
            initAlbumsTab(config);
        } else {
            initSettingsTab(config);
        }
    }

    private void switchTab(Tab tab) {
        currentTab = tab;
        this.rebuildWidgets();
    }

    // ==================== ВКЛАДКА "АЛЬБОМИ" ====================

    private void initAlbumsTab(MusicConfig config) {
        Album activeAlbum = config.getActiveAlbum();
        List<Album> allAlbums = config.getAlbums();

        // 1. Панель вибору та керування альбомом (y = 29)
        int albumBarY = 29;
        int center = this.width / 2;

        this.prevAlbumButton = Button.builder(Component.literal("◀"), b -> cycleAlbum(-1))
                .bounds(center - 165, albumBarY, 20, 20).build();
        this.addRenderableWidget(prevAlbumButton);

        String albumDisplay = activeAlbum.isPermanent()
                ? I18n.get("gui.custommusic.album.all_tracks")
                : activeAlbum.getName();
        this.currentAlbumButton = Button.builder(Component.literal("💿 " + albumDisplay), b -> cycleAlbum(1))
                .bounds(center - 142, albumBarY, 140, 20).build();
        this.addRenderableWidget(currentAlbumButton);

        this.nextAlbumButton = Button.builder(Component.literal("▶"), b -> cycleAlbum(1))
                .bounds(center + 1, albumBarY, 20, 20).build();
        this.addRenderableWidget(nextAlbumButton);

        this.newAlbumButton = Button.builder(
                Component.translatable("gui.custommusic.album.new"),
                b -> this.minecraft.setScreen(new CreateAlbumScreen(this))
        ).bounds(center + 24, albumBarY, 58, 20).build();
        this.addRenderableWidget(newAlbumButton);

        this.deleteAlbumButton = Button.builder(
                Component.literal("🗑"),
                b -> {
                    if (!activeAlbum.isPermanent()) {
                        config.deleteAlbum(activeAlbum.getId());
                        this.rebuildWidgets();
                    }
                }
        ).bounds(center + 85, albumBarY, 20, 20).build();
        this.deleteAlbumButton.active = !activeAlbum.isPermanent();
        this.addRenderableWidget(deleteAlbumButton);

        this.addSongToAlbumButton = Button.builder(
                Component.translatable("gui.custommusic.album.add_song"),
                b -> this.minecraft.setScreen(new TrackSelectionScreen(
                        this,
                        TrackSelectionScreen.Mode.ADD_TO_ALBUM,
                        activeAlbum,
                        Component.translatable("gui.custommusic.add_to_album_title")
                ))
        ).bounds(center + 108, albumBarY, 57, 20).build();
        this.addSongToAlbumButton.active = !activeAlbum.isPermanent();
        this.addRenderableWidget(addSongToAlbumButton);

        // 2. Закріплений інтерактивний міні-плеєр
        int playerW = 230;
        int playerH = 46;
        int playerX = (this.width - playerW) / 2;
        int playerY = albumBarY + 23;
        this.pinnedPlayer = new PinnedPlayerWidget(playerX, playerY, playerW, playerH);
        this.addRenderableWidget(pinnedPlayer);

        // 3. Панель дій над списком: Пошук + Упорядкувати + Перемішати (y = playerY + playerH + 4)
        int actionY = playerY + playerH + 4;
        int searchW = 105;
        this.searchBox = new EditBox(this.font, center - 165, actionY, searchW, 20, Component.translatable("gui.custommusic.search"));
        this.searchBox.setHint(Component.translatable("gui.custommusic.search").withStyle(ChatFormatting.DARK_GRAY));
        this.searchBox.setResponder(text -> {
            if (trackList != null) {
                trackList.setFilterQuery(text);
            }
        });
        this.addRenderableWidget(searchBox);

        this.sortButton = Button.builder(
                getSortButtonComponent(activeAlbum.getSortMode()),
                b -> {
                    String next = getNextSortMode(activeAlbum.getSortMode());
                    config.sortAlbum(activeAlbum, next);
                    updateSortButtonMessage();
                    refreshList();
                }
        ).bounds(center - 57, actionY, 125, 20).build();
        this.addRenderableWidget(sortButton);

        this.shuffleButton = Button.builder(
                Component.translatable("gui.custommusic.shuffle"),
                b -> {
                    config.shuffleAlbum(activeAlbum);
                    updateSortButtonMessage();
                    refreshList();
                }
        ).bounds(center + 71, actionY, 94, 20).build();
        this.addRenderableWidget(shuffleButton);

        // 4. Список треків
        int listTop = actionY + 23;
        int listBottom = this.height - 30;
        this.trackList = new TrackListWidget(this.minecraft, this.width, listBottom - listTop, listTop, 24, this);
        this.addRenderableWidget(trackList);

        // 5. Нижні кнопки: Наступний трек та Готово
        int bottomY = this.height - 25;
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

    private void cycleAlbum(int delta) {
        MusicConfig config = CustomMusicMod.getInstance().getConfig();
        if (config == null) return;
        List<Album> albums = config.getAlbums();
        if (albums.isEmpty()) return;

        Album active = config.getActiveAlbum();
        int idx = 0;
        for (int i = 0; i < albums.size(); i++) {
            if (albums.get(i).getId().equals(active.getId())) {
                idx = i;
                break;
            }
        }

        int nextIdx = (idx + delta) % albums.size();
        if (nextIdx < 0) nextIdx = albums.size() - 1;
        config.setActiveAlbum(albums.get(nextIdx));
        this.rebuildWidgets();
    }

    private String getNextSortMode(String current) {
        if ("name".equals(current)) return "duration";
        if ("duration".equals(current)) return "type";
        if ("type".equals(current)) return "custom";
        return "name";
    }

    public void updateSortButtonMessage() {
        if (sortButton != null) {
            MusicConfig config = CustomMusicMod.getInstance().getConfig();
            if (config != null) {
                sortButton.setMessage(getSortButtonComponent(config.getActiveAlbum().getSortMode()));
            }
        }
    }

    private Component getSortButtonComponent(String sortMode) {
        String key;
        if ("name".equals(sortMode)) {
            key = "gui.custommusic.sort.name";
        } else if ("duration".equals(sortMode)) {
            key = "gui.custommusic.sort.duration";
        } else if ("type".equals(sortMode)) {
            key = "gui.custommusic.sort.type";
        } else {
            key = "gui.custommusic.sort.custom";
        }
        return Component.translatable("gui.custommusic.sort", Component.translatable(key));
    }

    // ==================== ВКЛАДКА "НАЛАШТУВАННЯ" ====================

    private void initSettingsTab(MusicConfig config) {
        int centerLeft = this.width / 2 - 155;
        int centerRight = this.width / 2 + 5;
        int y = 29;

        // 1. Затримки між піснями
        this.minDelaySlider = new DelaySlider(
                centerLeft, y, 150, 20,
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
                centerRight, y, 150, 20,
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

        // 2. Гучність за категоріями (кастомна, ванільна, платівки)
        y += 23;
        int volWidth = 100;
        int volSpacing = 5;
        int totalVolW = volWidth * 3 + volSpacing * 2;
        int volStartX = (this.width - totalVolW) / 2;

        this.customVolumeSlider = new VolumeSlider(
                volStartX, y, volWidth, 20,
                I18n.get("gui.custommusic.volume.custom_short"),
                config.getCustomVolume(),
                val -> {
                    config.setCustomVolume(val);
                    CustomMusicPlayer.getInstance().updateCurrentTrackVolume();
                }
        );
        this.addRenderableWidget(customVolumeSlider);

        this.vanillaVolumeSlider = new VolumeSlider(
                volStartX + volWidth + volSpacing, y, volWidth, 20,
                I18n.get("gui.custommusic.volume.vanilla_short"),
                config.getVanillaVolume(),
                val -> {
                    config.setVanillaVolume(val);
                    CustomMusicPlayer.getInstance().updateCurrentTrackVolume();
                }
        );
        this.addRenderableWidget(vanillaVolumeSlider);

        this.discVolumeSlider = new VolumeSlider(
                volStartX + (volWidth + volSpacing) * 2, y, volWidth, 20,
                I18n.get("gui.custommusic.volume.disc_short"),
                config.getDiscVolume(),
                val -> {
                    config.setDiscVolume(val);
                    CustomMusicPlayer.getInstance().updateCurrentTrackVolume();
                }
        );
        this.addRenderableWidget(discVolumeSlider);

        // 3. Додавання та видалення файлів музики
        y += 23;
        this.addMusicFileButton = Button.builder(
                Component.translatable("gui.custommusic.add_music"),
                b -> openAddMusicAction()
        ).bounds(centerLeft, y, 150, 20).build();
        this.addRenderableWidget(addMusicFileButton);

        this.deleteMusicFileButton = Button.builder(
                Component.translatable("gui.custommusic.delete_music"),
                b -> this.minecraft.setScreen(new TrackSelectionScreen(
                        this,
                        TrackSelectionScreen.Mode.DELETE_TRACK,
                        null,
                        Component.translatable("gui.custommusic.delete_title")
                ))
        ).bounds(centerRight, y, 150, 20).build();
        this.addRenderableWidget(deleteMusicFileButton);

        // 4. Кнопка налаштування індивідуальної гучності пісні
        y += 23;
        this.customizeTrackVolumeButton = Button.builder(
                Component.translatable("gui.custommusic.volume.adjust_song"),
                b -> this.minecraft.setScreen(new TrackSelectionScreen(
                        this,
                        TrackSelectionScreen.Mode.CUSTOMIZE_VOLUME,
                        null,
                        Component.translatable("gui.custommusic.select_track_volume_title")
                ))
        ).bounds(this.width / 2 - 155, y, 310, 20).build();
        this.addRenderableWidget(customizeTrackVolumeButton);

        // 5. Список пісень з індивідуальною гучністю
        int listTop = y + 23;
        int listBottom = this.height - 30;
        this.customVolumeListWidget = new CustomVolumeListWidget(
                this.minecraft, this.width, listBottom - listTop, listTop, 22, this
        );
        this.addRenderableWidget(customVolumeListWidget);

        // 6. Кнопка Готово
        int bottomY = this.height - 25;
        this.addRenderableWidget(Button.builder(
                CommonComponents.GUI_DONE,
                b -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(this.parentScreen);
                    }
                }
        ).bounds((this.width - 150) / 2, bottomY, 150, 20).build());
    }

    public void refreshList() {
        if (trackList != null) {
            trackList.refreshList();
        }
        if (customVolumeListWidget != null) {
            customVolumeListWidget.refreshEntries();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        // Якщо у вкладці налаштувань список індивідуальних гучностей порожній — відображаємо підказку
        if (currentTab == Tab.SETTINGS && customVolumeListWidget != null && customVolumeListWidget.isEmpty()) {
            Font font = this.font;
            Component emptyText = Component.translatable("gui.custommusic.volume.no_custom_tracks").withStyle(ChatFormatting.GRAY);
            graphics.drawCenteredString(font, emptyText, this.width / 2, this.height / 2 + 25, 0xFFAAAAAA);
        }

        if (statusComponent != null) {
            graphics.drawCenteredString(this.font, statusComponent, this.width / 2, this.height - 40, 0xFFFFFFFF);
        }
    }

    /**
     * Відкриває нативне системне вікно вибору файлів (Windows OpenFileDialog / провідник).
     */
    private void openAddMusicAction() {
        this.statusComponent = Component.translatable("gui.custommusic.status.choose_files").withStyle(ChatFormatting.YELLOW);

        CompletableFuture.runAsync(() -> {
            List<File> selectedFiles = new ArrayList<>();
            String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            String dialogTitle = I18n.get("gui.custommusic.dialog.title");
            String filterAudio = I18n.get("gui.custommusic.dialog.filter_audio");
            String filterAll = I18n.get("gui.custommusic.dialog.filter_all");

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
                } catch (Exception e) {
                    LOGGER.error("[CustomMusic] Помилка запуску OpenFileDialog через PowerShell", e);
                }
            }

            // Fallback для інших ОС через Java AWT FileDialog
            if (selectedFiles.isEmpty() && !osName.contains("win")) {
                try {
                    java.awt.FileDialog fileDialog = new java.awt.FileDialog((java.awt.Frame) null, dialogTitle, java.awt.FileDialog.LOAD);
                    fileDialog.setMultipleMode(true);
                    fileDialog.setFilenameFilter((dir, name) -> {
                        String lower = name.toLowerCase(Locale.ROOT);
                        return lower.endsWith(".ogg") || lower.endsWith(".mp3");
                    });
                    fileDialog.setVisible(true);

                    File[] files = fileDialog.getFiles();
                    if (files != null) {
                        for (File f : files) {
                            if (f.isFile() && f.exists()) {
                                selectedFiles.add(f);
                            }
                        }
                    }
                    fileDialog.dispose();
                } catch (Throwable t) {
                    LOGGER.warn("[CustomMusic] Не вдалося відкрити системний діалог: {}", t.getMessage());
                }
            }

            if (selectedFiles.isEmpty()) {
                if (this.minecraft != null) {
                    this.minecraft.execute(() -> {
                        this.statusComponent = Component.translatable("gui.custommusic.status.no_new_tracks").withStyle(ChatFormatting.GRAY);
                    });
                }
                return;
            }

            Path tracksDir = CustomMusicMod.getInstance().getDirectoryManager().getTracksDir();
            int copiedCount = 0;
            for (File sourceFile : selectedFiles) {
                try {
                    String cleanName = sourceFile.getName().replaceAll("[\\\\/:*?\"<>|]", "_");
                    Path targetPath = tracksDir.resolve(cleanName);
                    Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                    copiedCount++;
                } catch (Exception e) {
                    LOGGER.error("[CustomMusic] Помилка копіювання файлу: " + sourceFile.getName(), e);
                }
            }

            final int finalCopied = copiedCount;
            if (this.minecraft != null) {
                this.minecraft.execute(() -> {
                    this.statusComponent = Component.translatable("gui.custommusic.status.copied_ready", finalCopied).withStyle(ChatFormatting.GREEN);
                    CustomMusicMod.getInstance().refreshTracks();
                    refreshList();
                });
            }
        });
    }
}
