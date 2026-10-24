package com.cat.custommusic.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Слайдер для налаштування затримки тиші між треками (у секундах).
 */
public class DelaySlider extends AbstractSliderButton {
    private final String translationKey;
    private final int minSec;
    private final int maxSec;
    private final Consumer<Integer> onValueChanged;

    public DelaySlider(int x, int y, int width, int height, String translationKey, int currentSec, int minSec, int maxSec, Consumer<Integer> onValueChanged) {
        super(x, y, width, height, Component.empty(), (double) (currentSec - minSec) / (maxSec - minSec));
        this.translationKey = translationKey;
        this.minSec = minSec;
        this.maxSec = maxSec;
        this.onValueChanged = onValueChanged;
        this.updateMessage();
    }

    public int getSeconds() {
        return (int) Math.round(this.minSec + this.value * (this.maxSec - this.minSec));
    }

    public void setSeconds(int seconds) {
        seconds = Math.max(minSec, Math.min(maxSec, seconds));
        this.value = (double) (seconds - minSec) / (maxSec - minSec);
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.translatable(translationKey, getSeconds()));
    }

    @Override
    protected void applyValue() {
        if (onValueChanged != null) {
            onValueChanged.accept(getSeconds());
        }
    }
}
