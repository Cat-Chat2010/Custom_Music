package com.cat.custommusic.gui;

import com.cat.custommusic.CustomMusicMod;
import com.cat.custommusic.config.Album;
import com.cat.custommusic.config.MusicConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Екран для створення нового альбому.
 * Якщо назву не введено, альбом автоматично іменується "Альбом N" починаючи з 0.
 */
public class CreateAlbumScreen extends Screen {
    private final Screen parentScreen;
    private EditBox nameBox;

    public CreateAlbumScreen(Screen parentScreen) {
        super(Component.translatable("gui.custommusic.album.create_title"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        int boxWidth = 220;
        int boxX = (this.width - boxWidth) / 2;
        int boxY = this.height / 2 - 20;

        this.nameBox = new EditBox(this.font, boxX, boxY, boxWidth, 20, Component.translatable("gui.custommusic.album.name_placeholder"));
        this.nameBox.setHint(Component.translatable("gui.custommusic.album.name_placeholder").withStyle(ChatFormatting.DARK_GRAY));
        this.addRenderableWidget(nameBox);
        this.setInitialFocus(nameBox);

        int btnWidth = 105;
        int btnY = boxY + 28;

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.custommusic.album.create_button"),
                b -> createAndReturn()
        ).bounds(boxX, btnY, btnWidth, 20).build());

        this.addRenderableWidget(Button.builder(
                CommonComponents.GUI_CANCEL,
                b -> this.minecraft.setScreen(this.parentScreen)
        ).bounds(boxX + btnWidth + 10, btnY, btnWidth, 20).build());
    }

    private void createAndReturn() {
        MusicConfig config = CustomMusicMod.getInstance().getConfig();
        if (config != null) {
            String enteredName = nameBox.getValue().trim();
            Album created = config.createAlbum(enteredName);
            config.setActiveAlbum(created);
        }
        this.minecraft.setScreen(this.parentScreen);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { // Enter / Numpad Enter
            createAndReturn();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 40, 0xFFFFFFFF);
    }
}
