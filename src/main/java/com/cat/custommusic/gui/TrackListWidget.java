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
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Прокручуваний список треків поточного активного альбому:
 * - Перетягування (Drag-and-Drop) порядку пісень (автоматично вмикає режим упорядкування "Своє").
 * - Кнопка [✔] / [✕] для увімкнення / вимкнення пісні з ротації.
 * - Пошукова фільтрація за назвою.
 * - У кастомних альбомах хрестик [✕] праворуч видаляє трек з цього альбому.
 * - В альбомі "Усі пісні" видалення вимкнено (хрестика немає).
 */
public class TrackListWidget extends ContainerObjectSelectionList<TrackListWidget.TrackEntryRow> {
    private final CustomMusicScreen parentScreen;
    private int draggedIndex = -1;
    private boolean isDraggingHandle = false;
    private String filterQuery = "";

    public TrackListWidget(Minecraft minecraft, int width, int height, int y, int itemHeight, CustomMusicScreen parentScreen) {
        super(minecraft, width, height, y, itemHeight);
        this.parentScreen = parentScreen;
        refreshList();
    }

    public void setFilterQuery(String query) {
        this.filterQuery = query != null ? query : "";
        refreshList();
    }

    public void refreshList() {
        this.clearEntries();
        MusicConfig config = CustomMusicMod.getInstance().getConfig();
        if (config == null) return;

        Album activeAlbum = config.getActiveAlbum();
        List<TrackEntry> tracks = config.getTracksForAlbum(activeAlbum);
        boolean isCustomAlbum = !activeAlbum.isPermanent();

        String q = filterQuery.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < tracks.size(); i++) {
            TrackEntry t = tracks.get(i);
            if (q.isEmpty() || t.getDisplayName().toLowerCase(Locale.ROOT).contains(q)) {
                this.addEntry(new TrackEntryRow(t, i, isCustomAlbum));
            }
        }
    }

    public void startDragging(int index) {
        this.draggedIndex = index;
        this.isDraggingHandle = true;
    }

    public boolean isDragging(int index) {
        return this.isDraggingHandle && this.draggedIndex == index;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingHandle && draggedIndex >= 0) {
            // Плавний автоскрол при підході до меж списку
            if (mouseY < this.getY() + 16) {
                this.setScrollAmount(this.getScrollAmount() - 5);
            } else if (mouseY > this.getY() + this.getHeight() - 16) {
                this.setScrollAmount(this.getScrollAmount() + 5);
            }

            int relativeY = (int) (mouseY - this.getY() + this.getScrollAmount() - 4);
            int targetIndex = relativeY / this.itemHeight;
            int maxIndex = getItemCount() - 1;
            if (targetIndex < 0) targetIndex = 0;
            if (targetIndex > maxIndex) targetIndex = maxIndex;

            if (targetIndex != draggedIndex && targetIndex >= 0 && targetIndex < getItemCount()) {
                Album activeAlbum = CustomMusicMod.getInstance().getConfig().getActiveAlbum();
                CustomMusicMod.getInstance().getConfig().moveAlbumTrack(activeAlbum, draggedIndex, targetIndex);
                this.draggedIndex = targetIndex;
                refreshList();
                parentScreen.updateSortButtonMessage();
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDraggingHandle) {
            isDraggingHandle = false;
            draggedIndex = -1;
            CustomMusicMod.getInstance().getConfig().save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public int getRowWidth() {
        return Math.min(360, this.width - 20);
    }

    public class TrackEntryRow extends ContainerObjectSelectionList.Entry<TrackEntryRow> {
        private final TrackEntry track;
        private final int index;
        private final Button toggleButton;
        private final Button removeButton;
        private final String durationText;
        private final List<AbstractWidget> children = new ArrayList<>();

        public TrackEntryRow(TrackEntry track, int index, boolean isCustomAlbum) {
            this.track = track;
            this.index = index;
            this.durationText = com.cat.custommusic.audio.OggAudioHelper.getTrackDurationFormatted(track);

            // Кнопка [✔] / [✕] для увімкнення / вимкнення треку в ротації альбому
            this.toggleButton = Button.builder(
                    Component.literal(track.isEnabled() ? "✔" : "✕"),
                    b -> {
                        CustomMusicMod.getInstance().getConfig().toggleTrack(track);
                        parentScreen.refreshList();
                        Minecraft.getInstance().getSoundManager().play(
                                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
                        );
                    }
            ).bounds(0, 0, 18, 18).build();
            children.add(toggleButton);

            // Кнопка прибирання з кастомного альбому (для альбому "Усі пісні" прибирання вимкнено)
            if (isCustomAlbum) {
                this.removeButton = Button.builder(
                        Component.literal("✕").withStyle(ChatFormatting.RED),
                        b -> {
                            Album active = CustomMusicMod.getInstance().getConfig().getActiveAlbum();
                            CustomMusicMod.getInstance().getConfig().removeTrackFromAlbum(active, track.getId());
                            parentScreen.refreshList();
                            Minecraft.getInstance().getSoundManager().play(
                                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
                            );
                        }
                ).bounds(0, 0, 18, 18).build();
                children.add(removeButton);
            } else {
                this.removeButton = null;
            }
        }

        public TrackEntry getTrack() {
            return track;
        }

        public int getIndex() {
            return index;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) { // Лівий клік миші
                // 1. Клік по кнопці увімкнення / вимкнення треку
                if (toggleButton.isMouseOver(mouseX, mouseY)) {
                    return toggleButton.mouseClicked(mouseX, mouseY, button);
                }

                // 2. Клік по кнопці прибирання з альбому
                if (removeButton != null && removeButton.isMouseOver(mouseX, mouseY)) {
                    return removeButton.mouseClicked(mouseX, mouseY, button);
                }

                int rowLeft = TrackListWidget.this.getRowLeft();
                // 3. Клік по смужках зліва (перші 14 пікселів) -> початок перетягування
                if (mouseX >= rowLeft && mouseX <= rowLeft + 14) {
                    TrackListWidget.this.startDragging(this.index);
                    return true;
                }

                // 4. Клік по рядку треку: запуск або пауза
                CustomMusicPlayer player = CustomMusicPlayer.getInstance();
                boolean isThisCurrent = track.equals(player.getCurrentTrack());
                if (isThisCurrent && player.isPlaying()) {
                    player.togglePlayPause();
                } else {
                    player.playTrack(track);
                }
                Minecraft.getInstance().getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
                );
                return true;
            }
            return false;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return children;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return children;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTick) {
            boolean beingDragged = TrackListWidget.this.isDragging(this.index);

            // Підсвічування рядка
            if (beingDragged) {
                graphics.fill(left, top, left + width, top + height, 0x4455FF55);
            } else if (isHovered) {
                graphics.fill(left, top, left + width, top + height, 0x1AFFFFFF);
            }

            // 1. Ручка перетягування (2 вертикальні білі/сірі смужки зліва)
            int handleX = left + 3;
            int handleY = top + 4;
            int handleHeight = height - 8;
            boolean hoverHandle = (mouseX >= left && mouseX <= left + 14 && mouseY >= top && mouseY <= top + height);
            int barColor = beingDragged ? 0xFF55FFFF : (hoverHandle ? 0xFFFFFFFF : 0xFF777777);

            graphics.fill(handleX, handleY, handleX + 2, handleY + handleHeight, barColor);
            graphics.fill(handleX + 4, handleY, handleX + 6, handleY + handleHeight, barColor);

            // 2. Кнопка біля назви для ввімкнення / вимкнення [✔] або [✕]
            toggleButton.setMessage(track.isEnabled()
                    ? Component.literal("✔").withStyle(ChatFormatting.GREEN)
                    : Component.literal("✕").withStyle(ChatFormatting.DARK_GRAY));
            toggleButton.setX(left + 14);
            toggleButton.setY(top + 3);
            toggleButton.render(graphics, mouseX, mouseY, partialTick);

            // 3. Статус відтворення
            CustomMusicPlayer player = CustomMusicPlayer.getInstance();
            boolean isThisCurrent = track.equals(player.getCurrentTrack()) && player.isPlaying();
            boolean isPaused = player.isPaused();

            // 4. Бейдж [Custom]/[Vanilla]/[Disc] та назва пісні
            Font font = Minecraft.getInstance().font;
            int textX = left + 36;
            String typeBadge;
            ChatFormatting badgeStyle;
            if (track.isCustom()) {
                typeBadge = "[Custom] ";
                badgeStyle = ChatFormatting.AQUA;
            } else if (track.isDisc()) {
                typeBadge = "[Disc] ";
                badgeStyle = ChatFormatting.GOLD;
            } else {
                typeBadge = "[Vanilla] ";
                badgeStyle = ChatFormatting.GRAY;
            }
            Component badgeComponent = Component.literal(typeBadge).withStyle(badgeStyle);

            Component nameComponent;
            if (track.isEnabled()) {
                if (isThisCurrent) {
                    nameComponent = Component.literal((isPaused ? "⏸ " : "🎵 ") + track.getDisplayName())
                            .withStyle(isPaused ? ChatFormatting.GOLD : ChatFormatting.YELLOW);
                } else {
                    nameComponent = Component.literal(track.getDisplayName()).withStyle(ChatFormatting.WHITE);
                }
            } else {
                nameComponent = Component.literal(track.getDisplayName()).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH);
            }

            int badgeWidth = font.width(badgeComponent);
            int durWidth = font.width(durationText);
            int rightMargin = (removeButton != null ? 24 : 6) + durWidth;
            int maxNameWidth = width - (textX - left) - rightMargin - badgeWidth;

            graphics.drawString(font, badgeComponent, textX, top + 6, 0xFFFFFFFF, false);

            String nameStr = nameComponent.getString();
            if (font.width(nameStr) > maxNameWidth) {
                nameStr = font.plainSubstrByWidth(nameStr, maxNameWidth - font.width("...")) + "...";
            }
            graphics.drawString(font, Component.literal(nameStr).withStyle(nameComponent.getStyle()), textX + badgeWidth, top + 6, 0xFFFFFFFF, false);

            // 5. Тривалість пісні праворуч
            int durX = left + width - durWidth - (removeButton != null ? 24 : 4);
            graphics.drawString(font, durationText, durX, top + 6, 0xFFAAAAAA, false);

            // 6. Кнопка [✕] прибирання з альбому
            if (removeButton != null) {
                removeButton.setX(left + width - 20);
                removeButton.setY(top + 3);
                removeButton.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }
}
