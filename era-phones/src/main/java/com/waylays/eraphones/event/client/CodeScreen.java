package com.waylays.eraphones.event.client;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.event.EventNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * The code box's front, close up: a plate of old steel with its brass number wheels behind little windows. Click the
 * top of a wheel (or scroll on it) to turn it up, the bottom to turn it down, or type the digits; then the brass
 * handle. A game master in creative sees the box's code and can make the wheels' number its new code.
 */
public class CodeScreen extends Screen {
    private static final ResourceLocation PANEL = EraPhones.id("textures/gui/event/boite_code.png");
    private static final ResourceLocation DIGITS = EraPhones.id("textures/gui/event/boite_code_chiffres.png");
    /** The plate (gui units), a wheel's window, a digit's height on the wheel, the handle and the game master's button. */
    private static final int PW = 236, PH = 150, WW = 26, WH = 40, CELL = 28, GAP = 6, WY = 44;
    private static final int OPEN_W = 84, OPEN_H = 22, OPEN_Y = 108;

    private final BlockPos pos;
    private final int[] wheel;
    private final float[] spin;
    private final String known;
    private int typed;
    private float scale = 1;
    private int ox, oy;
    private long lastFrame;

    public CodeScreen(BlockPos pos, int digits, String known) {
        super(Component.translatable("block.eraphones.boite_code"));
        this.pos = pos;
        this.wheel = new int[Math.max(1, Math.min(6, digits))];
        this.spin = new float[wheel.length];
        this.known = known;
    }

    public static void open(EventNet.OpenCode p) {
        Minecraft.getInstance().setScreen(new CodeScreen(p.pos(), p.digits(), p.known()));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        scale = Math.min(2f, Math.min((width - 16) / (float) PW, (height - 16) / (float) PH));
        scale = Math.max(.5f, (float) Math.floor(scale * 2) / 2);
        ox = (int) ((width - PW * scale) / 2);
        oy = (int) ((height - PH * scale) / 2);
    }

    private double ux(double mx) {
        return (mx - ox) / scale;
    }

    private double uy(double my) {
        return (my - oy) / scale;
    }

    private int wheelX(int i) {
        int total = wheel.length * WW + (wheel.length - 1) * GAP;
        return (PW - total) / 2 + i * (WW + GAP);
    }

    private boolean creative() {
        return !known.isEmpty();
    }

    private int openX() {
        return creative() ? PW / 2 - OPEN_W - 4 : (PW - OPEN_W) / 2;
    }

    private String code() {
        StringBuilder b = new StringBuilder();
        for (int d : wheel) {
            b.append(d);
        }
        return b.toString();
    }

    // ---------------------------------------------------------------- turning the wheels

    private void turn(int i, int by) {
        wheel[i] = Math.floorMod(wheel[i] + by, 10);
        spin[i] = -by;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.LEVER_CLICK, 1.9f + (wheel[i] % 3) * .05f, .35f));
    }

    private void handle(boolean set) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.IRON_TRAPDOOR_CLOSE, .8f, .5f));
        PacketDistributor.sendToServer(new EventNet.TryCode(pos, code(), set));
        if (!set) {
            onClose();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        double x = ux(mx), y = uy(my);
        for (int i = 0; i < wheel.length; i++) {
            int wx = wheelX(i);
            if (x >= wx - 2 && x < wx + WW + 2 && y >= WY - 14 && y < WY + WH + 14) {
                turn(i, y < WY + WH / 2.0 ? 1 : -1);
                return true;
            }
        }
        if (in(x, y, openX(), OPEN_Y, OPEN_W, OPEN_H)) {
            handle(false);
            return true;
        }
        if (creative() && in(x, y, PW / 2 + 4, OPEN_Y, OPEN_W, OPEN_H)) {
            handle(true);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        double x = ux(mx);
        for (int i = 0; i < wheel.length; i++) {
            int wx = wheelX(i);
            if (x >= wx - GAP / 2.0 && x < wx + WW + GAP / 2.0 && dy != 0) {
                turn(i, dy > 0 ? 1 : -1);
                return true;
            }
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (c >= '0' && c <= '9') {
            int i = typed % wheel.length;
            int by = (c - '0') - wheel[i];
            if (by != 0) {
                turn(i, by);
                spin[i] = Math.signum(-by);
            }
            typed++;
            return true;
        }
        return super.charTyped(c, mods);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            handle(false);
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            typed = Math.max(0, typed - 1);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private static boolean in(double x, double y, int x0, int y0, int w, int h) {
        return x >= x0 && x < x0 + w && y >= y0 && y < y0 + h;
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0xA0000000);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        long now = System.currentTimeMillis();
        float dt = lastFrame == 0 ? 0 : Math.min(.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        double x = ux(mx), y = uy(my);
        g.pose().pushPose();
        g.pose().translate(ox, oy, 0);
        g.pose().scale(scale, scale, 1);
        g.blit(PANEL, 0, 0, PW, PH, 0, 0, PW * 2, PH * 2, PW * 2, PH * 2);
        for (int i = 0; i < wheel.length; i++) {
            // The wheel rolls to its digit
            spin[i] *= Math.max(0, 1 - dt * 14);
            if (Math.abs(spin[i]) < .01f) {
                spin[i] = 0;
            }
            drawWheel(g, i, x, y);
        }
        plate(g, openX(), Component.translatable("event.eraphones.codebox.open").getString(), in(x, y, openX(), OPEN_Y, OPEN_W, OPEN_H), 0xFFE8C677);
        if (creative()) {
            plate(g, PW / 2 + 4, Component.translatable("event.eraphones.codebox.set").getString(), in(x, y, PW / 2 + 4, OPEN_Y, OPEN_W, OPEN_H),
                    0xFF9FD0FF);
            String k = Component.translatable("event.eraphones.codebox.known", known).getString();
            g.drawString(font, k, (PW - font.width(k)) / 2, PH - 14, 0xFF8A8F96, false);
        }
        g.pose().popPose();
    }

    private void drawWheel(GuiGraphics g, int i, double mx, double my) {
        int wx = wheelX(i);
        // The digits on the brass drum: the one in the window, the next ones rolling away above and below
        float v = (wheel[i] + 2) * CELL + CELL / 2f - WH / 2f + spin[i] * CELL;
        // The window it turns behind: a dark frame sunk in the brass
        g.fill(wx - 3, WY - 3, wx + WW + 3, WY + WH + 3, 0xFF2A1E0C);
        g.fill(wx - 2, WY - 2, wx + WW + 2, WY + WH + 2, 0xFF0E0A06);
        g.fill(wx - 3, WY + WH + 2, wx + WW + 3, WY + WH + 3, 0xFFE6C47A);
        g.blit(DIGITS, wx, WY, WW, WH, 0, v * 2, WW * 2, WH * 2, WW * 2, CELL * 2 * 14);
        // The drum's roundness: dark toward the top and bottom of the window
        g.fillGradient(wx, WY, wx + WW, WY + 12, 0xE0000000, 0x00000000);
        g.fillGradient(wx, WY + WH - 12, wx + WW, WY + WH, 0x00000000, 0xE0000000);
        g.fill(wx, WY + WH / 2 - 9, wx + WW, WY + WH / 2 - 8, 0x30FFFFFF);
        // The little arrows above and below, lit under the pointer
        boolean hover = mx >= wx - 2 && mx < wx + WW + 2;
        boolean up = hover && my >= WY - 14 && my < WY + WH / 2.0, down = hover && my >= WY + WH / 2.0 && my < WY + WH + 14;
        g.drawString(font, "▲", wx + (WW - font.width("▲")) / 2, WY - 11, up ? 0xFFFFE6A0 : 0xFFB08A3E, false);
        g.drawString(font, "▼", wx + (WW - font.width("▼")) / 2, WY + WH + 3, down ? 0xFFFFE6A0 : 0xFFB08A3E, false);
    }

    /** A brass plate to press: the handle, or the game master's. */
    private void plate(GuiGraphics g, int px, String text, boolean hover, int color) {
        int top = hover ? 0xFFD9B565 : 0xFFB8933F, bottom = hover ? 0xFF8C6A26 : 0xFF6E521C;
        g.fill(px - 1, OPEN_Y - 1, px + OPEN_W + 1, OPEN_Y + OPEN_H + 1, 0xFF2A2116);
        g.fillGradient(px, OPEN_Y, px + OPEN_W, OPEN_Y + OPEN_H, top, bottom);
        g.fill(px + 1, OPEN_Y + 1, px + OPEN_W - 1, OPEN_Y + 2, 0x60FFFFFF);
        // Two little screw heads
        g.fill(px + 3, OPEN_Y + OPEN_H / 2 - 1, px + 5, OPEN_Y + OPEN_H / 2 + 1, 0xFF3A2E1A);
        g.fill(px + OPEN_W - 5, OPEN_Y + OPEN_H / 2 - 1, px + OPEN_W - 3, OPEN_Y + OPEN_H / 2 + 1, 0xFF3A2E1A);
        g.drawString(font, text, px + (OPEN_W - font.width(text)) / 2 + 1, OPEN_Y + 8, 0x80000000, false);
        g.drawString(font, text, px + (OPEN_W - font.width(text)) / 2, OPEN_Y + 7, color == 0xFFE8C677 ? 0xFF2A1E0A : 0xFF10243A, false);
    }
}
