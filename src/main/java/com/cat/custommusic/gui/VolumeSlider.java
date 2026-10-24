package com.cat.custommusic.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Слайдер для налаштування гучності (0% - 100%).
 */
public class VolumeSlider extends AbstractSliderButton {
    private final String title;
    private final Consumer<Float> onValueChanged;

    public VolumeSlider(int x, int y, int width, int height, String title, float currentFactor, Consumer<Float> onValueChanged) {
        super(x, y, width, height, Component.empty(), currentFactor);
        this.title = title;
        this.onValueChanged = onValueChanged;
        this.updateMessage();
    }

    public int getPercent() {
        return (int) Math.round(this.value * 100.0);
    }

    public float getFactor() {
        return (float) this.value;
    }

    public void setFactor(float factor) {
        this.value = Math.max(0.0, Math.min(1.0, (double) factor));
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.literal(title + ": " + getPercent() + "%"));
    }

    @Override
    protected void applyValue() {
        if (onValueChanged != null) {
            onValueChanged.accept(getFactor());
        }
    }
}
