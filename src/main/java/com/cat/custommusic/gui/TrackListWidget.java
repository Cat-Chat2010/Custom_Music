package com.cat.custommusic.gui;

import com.cat.custommusic.CustomMusicMod;
import com.cat.custommusic.config.TrackEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Прокручуваний віджет списку треків.
 */
public class TrackListWidget extends ContainerObjectSelectionList<TrackListWidget.TrackEntryRow> {
    private final CustomMusicScreen parentScreen;

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

    @Override
    public int getRowWidth() {
        return Math.min(360, this.width - 20);
    }

    public class TrackEntryRow extends ContainerObjectSelectionList.Entry<TrackEntryRow> {
        private final TrackEntry track;
        private final int index;

        private final Button toggleButton;
        private final Button upButton;
        private final Button downButton;
        private final Button deleteButton;
        private final List<GuiEventListener> children = new ArrayList<>();

        public TrackEntryRow(TrackEntry track, int index) {
            this.track = track;
            this.index = index;

            // Кнопка Вкл / Вимк
            this.toggleButton = Button.builder(
                    getToggleText(),
                    b -> {
                        CustomMusicMod.getInstance().getConfig().toggleTrack(track);
                        b.setMessage(getToggleText());
                    }
            ).bounds(0, 0, 52, 20).build();

            // Кнопка Вгору
            this.upButton = Button.builder(
                    Component.literal("▲"),
                    b -> {
                        if (index > 0) {
                            CustomMusicMod.getInstance().getConfig().moveTrack(index, index - 1);
                            parentScreen.refreshList();
                        }
                    }
            ).bounds(0, 0, 20, 20).build();
            this.upButton.active = (index > 0);

            // Кнопка Вниз
            int totalTracks = CustomMusicMod.getInstance().getConfig().getTracks().size();
            this.downButton = Button.builder(
                    Component.literal("▼"),
                    b -> {
                        if (index < totalTracks - 1) {
                            CustomMusicMod.getInstance().getConfig().moveTrack(index, index + 1);
                            parentScreen.refreshList();
                        }
                    }
            ).bounds(0, 0, 20, 20).build();
            this.downButton.active = (index < totalTracks - 1);

            // Кнопка Видалення з диска (активна тільки для кастомних файлів)
            this.deleteButton = Button.builder(
                    Component.literal("✕").withStyle(ChatFormatting.RED),
                    b -> {
                        if (track.isCustom()) {
                            CustomMusicMod.getInstance().getDirectoryManager().deleteTrackFile(track.getFileName());
                            CustomMusicMod.getInstance().getConfig().removeCustomTrack(track);
                            parentScreen.refreshList();
                        }
                    }
            ).bounds(0, 0, 20, 20).build();
            this.deleteButton.active = track.isCustom();

            children.add(toggleButton);
            children.add(upButton);
            children.add(downButton);
            children.add(deleteButton);
        }

        private Component getToggleText() {
            return track.isEnabled()
                    ? Component.literal("✓ Вкл").withStyle(ChatFormatting.GREEN)
                    : Component.literal("✕ Вимк").withStyle(ChatFormatting.RED);
        }

        @Override
        public void render(GuiGraphics graphics, int entryIdx, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float partialTick) {
            Font font = Minecraft.getInstance().font;

            // Назва треку та позначка типу
            String typeBadge = track.isCustom() ? "[Custom] " : "[Vanilla] ";
            Component title = Component.literal(typeBadge).withStyle(track.isCustom() ? ChatFormatting.AQUA : ChatFormatting.GRAY)
                    .append(Component.literal(track.getDisplayName()).withStyle(track.isEnabled() ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY));

            // Обрізаємо занадто довгі назви, щоб помістилися
            graphics.drawString(font, title, left, top + 6, 0xFFFFFF);

            // Розміщення кнопок праворуч
            int buttonX = left + entryWidth - 120;
            toggleButton.setX(buttonX);
            toggleButton.setY(top);
            toggleButton.render(graphics, mouseX, mouseY, partialTick);

            upButton.setX(buttonX + 54);
            upButton.setY(top);
            upButton.render(graphics, mouseX, mouseY, partialTick);

            downButton.setX(buttonX + 76);
            downButton.setY(top);
            downButton.render(graphics, mouseX, mouseY, partialTick);

            deleteButton.setX(buttonX + 98);
            deleteButton.setY(top);
            deleteButton.render(graphics, mouseX, mouseY, partialTick);
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
