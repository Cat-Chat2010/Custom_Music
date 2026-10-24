package com.cat.custommusic.gui;

import com.cat.custommusic.CustomMusicMod;
import com.cat.custommusic.audio.CustomMusicPlayer;
import com.cat.custommusic.config.Album;
import com.cat.custommusic.config.MusicConfig;
import com.cat.custommusic.config.TrackEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Універсальний екран вибору треків:
 * - Додавання пісні до альбому.
 * - Вибір пісні для налаштування індивідуальної гучності.
 * - Видалення кастомних пісень з диску з пошуком та кнопкою [✕].
 */
public class TrackSelectionScreen extends Screen {
    public enum Mode {
        ADD_TO_ALBUM,
        CUSTOMIZE_VOLUME,
        DELETE_TRACK
    }

    private final Screen parentScreen;
    private final Mode mode;
    private final Album targetAlbum;
    private EditBox searchBox;
    private TrackSelectList listWidget;

    public TrackSelectionScreen(Screen parentScreen, Mode mode, Album targetAlbum, Component title) {
        super(title);
        this.parentScreen = parentScreen;
        this.mode = mode;
        this.targetAlbum = targetAlbum;
    }

    @Override
    protected void init() {
        // Поле пошуку
        int searchWidth = 240;
        int searchX = (this.width - searchWidth) / 2;
        this.searchBox = new EditBox(this.font, searchX, 22, searchWidth, 18, Component.translatable("gui.custommusic.search"));
        this.searchBox.setHint(Component.translatable("gui.custommusic.search").withStyle(ChatFormatting.DARK_GRAY));
        this.searchBox.setResponder(text -> refreshTracks());
        this.addRenderableWidget(searchBox);

        // Список треків
        int listTop = 46;
        int listBottom = this.height - 34;
        this.listWidget = new TrackSelectList(this.minecraft, this.width, listBottom - listTop, listTop, 22);
        this.addRenderableWidget(listWidget);

        // Кнопка повернення
        this.addRenderableWidget(Button.builder(
                CommonComponents.GUI_DONE,
                b -> this.minecraft.setScreen(this.parentScreen)
        ).bounds((this.width - 150) / 2, this.height - 28, 150, 20).build());

        refreshTracks();
    }

    public void refreshTracks() {
        if (listWidget == null) return;
        listWidget.clearEntries();

        MusicConfig config = CustomMusicMod.getInstance().getConfig();
        if (config == null) return;

        List<TrackEntry> availableTracks = new ArrayList<>();
        if (mode == Mode.ADD_TO_ALBUM) {
            List<String> existingIds = targetAlbum != null ? targetAlbum.getTrackIds() : Collections.emptyList();
            for (TrackEntry t : config.getTracks()) {
                if (!existingIds.contains(t.getId())) {
                    availableTracks.add(t);
                }
            }
        } else if (mode == Mode.DELETE_TRACK) {
            for (TrackEntry t : config.getTracks()) {
                if (t.isCustom()) {
                    availableTracks.add(t);
                }
            }
        } else { // CUSTOMIZE_VOLUME
            availableTracks.addAll(config.getTracks());
        }

        // Сортуємо за назвою
        availableTracks.sort(Comparator.comparing(t -> t.getDisplayName().toLowerCase(Locale.ROOT)));

        // Фільтруємо за пошуковим запитом
        String query = searchBox != null ? searchBox.getValue().trim().toLowerCase(Locale.ROOT) : "";
        for (TrackEntry t : availableTracks) {
            if (query.isEmpty() || t.getDisplayName().toLowerCase(Locale.ROOT).contains(query)) {
                listWidget.addTrack(t);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);
    }

    public class TrackSelectList extends ContainerObjectSelectionList<TrackSelectList.Entry> {
        public TrackSelectList(Minecraft mc, int width, int height, int top, int itemHeight) {
            super(mc, width, height, top, itemHeight);
        }

        public void clearEntries() {
            super.clearEntries();
        }

        public void addTrack(TrackEntry track) {
            this.addEntry(new Entry(track));
        }

        @Override
        public int getRowWidth() {
            return Math.min(360, this.width - 20);
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getRowLeft() + this.getRowWidth() + 4;
        }

        public class Entry extends ContainerObjectSelectionList.Entry<Entry> {
            private final TrackEntry track;
            private final Button actionButton;

            public Entry(TrackEntry track) {
                this.track = track;
                if (mode == Mode.DELETE_TRACK) {
                    this.actionButton = Button.builder(
                            Component.literal("✕").withStyle(ChatFormatting.RED),
                            b -> {
                                CustomMusicMod.getInstance().getDirectoryManager().deleteTrackFile(track.getFileName());
                                CustomMusicMod.getInstance().getConfig().removeCustomTrack(track);
                                refreshTracks();
                                Minecraft.getInstance().getSoundManager().play(
                                        SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
                                );
                            }
                    ).bounds(0, 0, 18, 18).build();
                } else {
                    this.actionButton = null;
                }
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return actionButton != null ? Collections.singletonList(actionButton) : Collections.emptyList();
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return actionButton != null ? Collections.singletonList(actionButton) : Collections.emptyList();
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button != 0) return false;

                if (actionButton != null && actionButton.isMouseOver(mouseX, mouseY)) {
                    return actionButton.mouseClicked(mouseX, mouseY, button);
                }

                if (mode == Mode.ADD_TO_ALBUM) {
                    CustomMusicMod.getInstance().getConfig().addTrackToAlbum(targetAlbum, track.getId());
                    Minecraft.getInstance().getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
                    );
                    Minecraft.getInstance().setScreen(parentScreen);
                    return true;
                } else if (mode == Mode.CUSTOMIZE_VOLUME) {
                    CustomMusicMod.getInstance().getConfig().setTrackVolume(track.getId(), 0.8f);
                    CustomMusicPlayer.getInstance().updateCurrentTrackVolume();
                    Minecraft.getInstance().getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
                    );
                    Minecraft.getInstance().setScreen(parentScreen);
                    return true;
                }
                return false;
            }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTick) {
                boolean rowHover = mouseX >= left && mouseX <= left + width && mouseY >= top && mouseY <= top + height;
                if (rowHover) {
                    graphics.fill(left, top, left + width, top + height, 0x22FFFFFF);
                }

                Font font = Minecraft.getInstance().font;

                // Бейдж
                String badge;
                ChatFormatting badgeColor;
                if (track.isCustom()) {
                    badge = "[Custom] ";
                    badgeColor = ChatFormatting.AQUA;
                } else if (track.isDisc()) {
                    badge = "[Disc] ";
                    badgeColor = ChatFormatting.GOLD;
                } else {
                    badge = "[Vanilla] ";
                    badgeColor = ChatFormatting.GRAY;
                }

                graphics.drawString(font, Component.literal(badge).withStyle(badgeColor), left + 4, top + 5, 0xFFFFFFFF, false);

                int badgeWidth = font.width(badge);
                graphics.drawString(font, track.getDisplayName(), left + 4 + badgeWidth, top + 5, rowHover ? 0xFFFFFF55 : 0xFFFFFFFF, false);

                if (actionButton != null) {
                    actionButton.setX(left + width - 22);
                    actionButton.setY(top + 2);
                    actionButton.render(graphics, mouseX, mouseY, partialTick);
                }
            }
        }
    }
}
