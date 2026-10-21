package com.cat.custommusic.gui;

import com.cat.custommusic.CustomMusicMod;
import com.cat.custommusic.audio.CustomMusicPlayer;
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
import java.util.List;

/**
 * Прокручуваний віджет списку треків:
 * 1. 2 вертикальні смужки зліва — перетягування (Drag-and-Drop).
 * 2. Кнопка [▶] / [⏸] біля назви — миттєвий запуск або зупинка треку.
 * 3. Клік по назві треку — увімкнення / вимкнення з ротації плейлиста.
 * 4. Кнопка [✕] праворуч — видалення кастомного треку з диска.
 */
public class TrackListWidget extends ContainerObjectSelectionList<TrackListWidget.TrackEntryRow> {
    private final CustomMusicScreen parentScreen;
    private int draggedIndex = -1;
    private boolean isDraggingHandle = false;

    public TrackListWidget(Minecraft minecraft, int width, int height, int y, int itemHeight, CustomMusicScreen parentScreen) {
        super(minecraft, width, height, y, itemHeight);
        this.parentScreen = parentScreen;
        refreshList();
    }

    public void refreshList() {
        this.clearEntries();
        List<TrackEntry> tracks = CustomMusicMod.getInstance().getConfig().getTracks();
        for (int i = 0; i < tracks.size(); i++) {
            this.addEntry(new TrackEntryRow(tracks.get(i), i));
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
            // Плавний автоскрол при підході до країв
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
                CustomMusicMod.getInstance().getConfig().moveTrack(draggedIndex, targetIndex);
                this.draggedIndex = targetIndex;
                refreshList();
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
        private final Button playButton;
        private final Button deleteButton;
        private final List<AbstractWidget> children = new ArrayList<>();

        public TrackEntryRow(TrackEntry track, int index) {
            this.track = track;
            this.index = index;

            // Кнопка [▶] / [⏸] для миттєвого відтворення або зупинки треку
            this.playButton = Button.builder(
                    Component.literal("▶"),
                    b -> {
                        CustomMusicPlayer player = CustomMusicPlayer.getInstance();
                        if (player.isPlaying() && track.equals(player.getCurrentTrack())) {
                            player.stopPlaying();
                        } else {
                            player.playTrack(track);
                        }
                    }
            ).bounds(0, 0, 18, 18).build();
            children.add(playButton);

            // Кнопка видалення кастомного файлу
            if (track.isCustom()) {
                this.deleteButton = Button.builder(
                        Component.literal("✕").withStyle(ChatFormatting.RED),
                        b -> {
                            CustomMusicMod.getInstance().getDirectoryManager().deleteTrackFile(track.getFileName());
                            CustomMusicMod.getInstance().getConfig().removeCustomTrack(track);
                            parentScreen.refreshList();
                        }
                ).bounds(0, 0, 18, 18).build();
                children.add(deleteButton);
            } else {
                this.deleteButton = null;
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
                // 1. Клік по кнопці Play / Pause
                if (playButton.isMouseOver(mouseX, mouseY)) {
                    return playButton.mouseClicked(mouseX, mouseY, button);
                }

                // 2. Клік по кнопці видалення (для кастомних)
                if (deleteButton != null && deleteButton.isMouseOver(mouseX, mouseY)) {
                    return deleteButton.mouseClicked(mouseX, mouseY, button);
                }

                int rowLeft = TrackListWidget.this.getRowLeft();
                // 3. Клік по смужках зліва (перші 14 пікселів) -> початок перетягування
                if (mouseX >= rowLeft && mouseX <= rowLeft + 14) {
                    TrackListWidget.this.startDragging(this.index);
                    return true;
                }

                // 4. Клік по рядку треку -> Увімкнути / Вимкнути з плейлиста
                CustomMusicMod.getInstance().getConfig().toggleTrack(track);
                Minecraft.getInstance().getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
                );
                return true;
            }
            return false;
        }

        @Override
        public void render(GuiGraphics graphics, int entryIdx, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float partialTick) {
            Font font = Minecraft.getInstance().font;
            boolean beingDragged = TrackListWidget.this.isDragging(this.index);

            // Підсвічування рядка при ховері або перетягуванні
            if (beingDragged) {
                graphics.fill(left, top, left + entryWidth, top + entryHeight, 0x4055FFFF);
            } else if (isHovered) {
                graphics.fill(left, top, left + entryWidth, top + entryHeight, 0x18FFFFFF);
            }

            // 1. Дві тонкі вертикальні смужки зліва (ручка для Drag-and-Drop)
            int handleX = left + 3;
            int handleY = top + 4;
            int handleHeight = entryHeight - 8;
            boolean hoverHandle = (mouseX >= left && mouseX <= left + 14 && mouseY >= top && mouseY <= top + entryHeight);
            int barColor = beingDragged ? 0xFF55FFFF : (hoverHandle ? 0xFFFFFFFF : 0xFF777777);

            graphics.fill(handleX, handleY, handleX + 2, handleY + handleHeight, barColor);
            graphics.fill(handleX + 4, handleY, handleX + 6, handleY + handleHeight, barColor);

            // 2. Кнопка [▶] або [⏸]
            CustomMusicPlayer player = CustomMusicPlayer.getInstance();
            boolean isThisPlaying = player.isPlaying() && track.equals(player.getCurrentTrack());
            playButton.setMessage(isThisPlaying
                    ? Component.literal("⏸").withStyle(ChatFormatting.GREEN)
                    : Component.literal("▶").withStyle(ChatFormatting.WHITE));
            playButton.setX(left + 14);
            playButton.setY(top + 3);
            playButton.render(graphics, mouseX, mouseY, partialTick);

            // 3. Статус у плейлисті (активний / вимкнений)
            int statusX = left + 36;
            if (track.isEnabled()) {
                graphics.drawString(font, "✔", statusX, top + 7, 0x55FF55);
            } else {
                graphics.drawString(font, "✕", statusX, top + 7, 0x888888);
            }

            // 4. Бейдж [Custom]/[Vanilla] та назва пісні
            int textX = left + 48;
            String typeBadge = track.isCustom() ? "[Custom] " : "[Vanilla] ";
            Component badgeComponent = Component.literal(typeBadge).withStyle(track.isCustom() ? ChatFormatting.AQUA : ChatFormatting.GRAY);

            Component nameComponent;
            if (track.isEnabled()) {
                nameComponent = Component.literal(track.getDisplayName()).withStyle(isThisPlaying ? ChatFormatting.YELLOW : ChatFormatting.WHITE);
            } else {
                nameComponent = Component.literal(track.getDisplayName()).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH);
            }

            Component title = Component.empty().append(badgeComponent).append(nameComponent);
            graphics.drawString(font, title, textX, top + 7, 0xFFFFFF);

            // 5. Кнопка видалення праворуч
            if (deleteButton != null) {
                deleteButton.setX(left + entryWidth - 22);
                deleteButton.setY(top + 3);
                deleteButton.render(graphics, mouseX, mouseY, partialTick);
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return children;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return children;
        }
    }
}
