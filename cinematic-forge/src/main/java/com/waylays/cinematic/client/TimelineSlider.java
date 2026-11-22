package com.waylays.cinematic.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.IntSupplier;

/** The scene's timeline: drags the local cursor (seconds shown), follows the scene while it plays. */
public class TimelineSlider extends AbstractSliderButton {
    private final IntSupplier length;
    private int[] marks = new int[0];
    private int[] accent = new int[0];

    public TimelineSlider(int x, int y, int w, int h, IntSupplier length) {
        super(x, y, w, h, Component.empty(), 0);
        this.length = length;
        sync();
    }

    public static String time(float ticks) {
        float s = ticks / 20f;
        int m = (int) (s / 60);
        return String.format(Locale.ROOT, "%d:%05.2f", m, s - m * 60);
    }

    public void setMarks(int[] marks, int[] accent) {
        this.marks = marks;
        this.accent = accent;
    }

    private int len() {
        return Math.max(20, length.getAsInt());
    }

    /** Puts the knob where the scene is. */
    public void sync() {
        value = Math.max(0, Math.min(1, ClientStudio.sceneTime(0f) / len()));
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.literal(time(ClientStudio.sceneTime(0f)) + " / " + time(len())));
    }

    @Override
    protected void applyValue() {
        if (ClientStudio.serverPlaying()) return;
        ClientStudio.previewing = false;
        ClientStudio.cursor = Math.round(value * len());
    }

    @Override
    public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
        super.renderWidget(g, mx, my, pt);
        int len = len();
        for (int m : marks) mark(g, m, len, 0xFFB0B0B0);
        for (int m : accent) mark(g, m, len, 0xFFFFC23D);
    }

    private void mark(GuiGraphics g, int tick, int len, int color) {
        int x = getX() + 4 + (int) ((getWidth() - 8) * Math.min(1f, tick / (float) len));
        g.fill(x, getY() - 4, x + 1, getY(), color);
    }
}
