package com.cat.custommusic.gui;

import com.cat.custommusic.CustomMusicMod;
import com.cat.custommusic.audio.CustomMusicPlayer;
import com.cat.custommusic.config.MusicConfig;
import com.cat.custommusic.config.TrackEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.*;

/**
 * Прокручуваний список пісень з індивідуально налаштованою гучністю у вкладці "Налаштування".
 * Якщо гучність підняти до 100%, пісня автоматично зникає зі списку.
 */
public class CustomVolumeListWidget extends ContainerObjectSelectionList<CustomVolumeListWidget.Entry> {
    private final CustomMusicScreen parentScreen;

    public CustomVolumeListWidget(Minecraft mc, int width, int height, int top, int itemHeight, CustomMusicScreen parentScreen) {
        super(mc, width, height, top, itemHeight);
        this.parentScreen = parentScreen;
        refreshEntries();
    }

    public void refreshEntries() {
        this.clearEntries();
        MusicConfig config = CustomMusicMod.getInstance().getConfig();
        if (config == null) return;

        Map<String, Float> volumes = config.getTrackVolumes();
        List<TrackEntry> customizedTracks = new ArrayList<>();
        for (String id : volumes.keySet()) {
            TrackEntry t = config.getTrackById(id);
            if (t != null) {
                customizedTracks.add(t);
            }
        }

        // Сортуємо список за назвою пісні
        customizedTracks.sort(Comparator.comparing(t -> t.getDisplayName().toLowerCase(Locale.ROOT)));

        for (TrackEntry t : customizedTracks) {
            float curVol = config.getTrackVolume(t.getId());
            this.addEntry(new Entry(t, curVol));
        }
    }

    public boolean isEmpty() {
        return this.getItemCount() == 0;
    }

    @Override
    public int getRowWidth() {
        return Math.min(340, this.width - 24);
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getRowLeft() + this.getRowWidth() + 4;
    }

    public class Entry extends ContainerObjectSelectionList.Entry<Entry> {
        private final TrackEntry track;
        private final VolumeSlider slider;

        public Entry(TrackEntry track, float initialVolume) {
            this.track = track;
            this.slider = new VolumeSlider(
                    0, 0, 110, 18,
                    "Гучність",
                    initialVolume,
                    val -> {
                        MusicConfig config = CustomMusicMod.getInstance().getConfig();
                        if (config != null) {
                            config.setTrackVolume(track.getId(), val);
                            CustomMusicPlayer.getInstance().updateCurrentTrackVolume();
                            // Якщо гучність 100%, оновлюємо список щоб пісня зникла
                            if (Math.abs(val - 1.0f) < 0.005f) {
                                refreshEntries();
                            }
                        }
                    }
            );
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return Collections.singletonList(slider);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return Collections.singletonList(slider);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTick) {
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

            graphics.drawString(font, Component.literal(badge).withStyle(badgeColor), left + 2, top + 5, 0xFFFFFFFF, false);

            int badgeWidth = font.width(badge);
            int maxNameWidth = width - 120 - badgeWidth - 10;
            String name = track.getDisplayName();
            if (font.width(name) > maxNameWidth) {
                name = font.plainSubstrByWidth(name, maxNameWidth - font.width("...")) + "...";
            }
            graphics.drawString(font, name, left + 2 + badgeWidth, top + 5, 0xFFFFFFFF, false);

            slider.setX(left + width - 114);
            slider.setY(top + 1);
            slider.render(graphics, mouseX, mouseY, partialTick);
        }
    }
}
