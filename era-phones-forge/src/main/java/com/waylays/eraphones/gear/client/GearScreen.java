package com.waylays.eraphones.gear.client;

import com.waylays.eraphones.client.PhoneUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * The face of a thing, drawn at a fixed size (in its own units) scaled to fit the window, with keys that light up
 * under the mouse and click when pressed.
 */
public abstract class GearScreen extends Screen {
    protected final int bodyW, bodyH;
    protected float scale, px, py;
    protected final List<Key> keys = new ArrayList<>();
    protected int mouseX, mouseY;

    /** A key: where it is (body units), what it does. */
    protected record Key(String id, int x, int y, int w, int h) {
        boolean in(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    protected GearScreen(String title, int bodyW, int bodyH) {
        super(Component.translatable(title));
        this.bodyW = bodyW;
        this.bodyH = bodyH;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void place() {
        scale = Math.min(Math.min((width - 20f) / bodyW, (height - 20f) / bodyH), 2.5f);
        px = (width - bodyW * scale) / 2;
        py = (height - bodyH * scale) / 2;
    }

        public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        g.fillGradient(0, 0, width, height, 0x70000000, 0xA0000000);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g, mx, my, pt);
        place();
        mouseX = (int) Math.floor((mx - px) / scale);
        mouseY = (int) Math.floor((my - py) / scale);
        g.pose().pushPose();
        g.pose().translate(px, py, 0);
        g.pose().scale(scale, scale, 1);
        PhoneUI.glow(g, bodyW / 2f, bodyH / 2f, bodyW * 1.3f, 0x30000000);
        draw(g, System.currentTimeMillis());
        g.pose().popPose();
    }

    protected abstract void draw(GuiGraphics g, long now);

    protected abstract void pressed(String id);

    protected boolean hover(Key k) {
        return k.in(mouseX, mouseY);
    }

    protected Key key(String id) {
        for (Key k : keys) {
            if (k.id.equals(id)) {
                return k;
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        double lx = (mx - px) / scale, ly = (my - py) / scale;
        for (Key k : keys) {
            if (k.in(lx, ly) && active(k)) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.STONE_BUTTON_CLICK_ON, 1.6f, .35f));
                pressed(k.id);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (Minecraft.getInstance().options.keyInventory.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    // ---------------------------------------------------------------- drawing helpers

    /** A face drawn by the art tools: two texture pixels to a unit, laid at the body's corner. */
    protected static void panel(GuiGraphics g, net.minecraft.resources.ResourceLocation tex, int x, int y, int w, int h) {
        PhoneUI.blend();
        g.blit(tex, x, y, w, h, 0, 0, w * 2, h * 2, w * 2, h * 2);
    }

    /** A sprite of {@code size} units centred on ({@code cx}, {@code cy}), turned {@code degrees}. */
    protected static void sprite(GuiGraphics g, net.minecraft.resources.ResourceLocation tex, float cx, float cy, float size, int texSize, float degrees) {
        PhoneUI.blend();
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(degrees));
        g.pose().scale(size / texSize, size / texSize, 1);
        g.blit(tex, -texSize / 2, -texSize / 2, 0, 0, texSize, texSize, texSize, texSize);
        g.pose().popPose();
    }

    /** Scissors to a rectangle of the body (in its units). */
    protected void clip(GuiGraphics g, float x0, float y0, float x1, float y1) {
        g.enableScissor(Math.round(px + x0 * scale), Math.round(py + y0 * scale), Math.round(px + x1 * scale), Math.round(py + y1 * scale));
    }

    /** The light of a key under the mouse (or held lit). */
    protected void lit(GuiGraphics g, Key k, int argb) {
        PhoneUI.round(g, k.x(), k.y(), k.w(), k.h(), Math.min(k.w(), k.h()) / 3, argb);
    }

    /** Whether a key is there to press now. */
    protected boolean active(Key k) {
        return true;
    }

    protected void hoverLight(GuiGraphics g) {
        for (Key k : keys) {
            if (hover(k) && active(k)) {
                lit(g, k, 0x30FFFFFF);
            }
        }
    }

    /** A round button of {@code color}, pressed-in under the mouse, its {@code label} on it. */
    protected void button(GuiGraphics g, Key k, int color, String label, int labelColor) {
        boolean h = hover(k);
        PhoneUI.round(g, k.x(), k.y() + 1, k.w(), k.h(), Math.min(k.w(), k.h()) / 2, 0x90000000);
        int top = PhoneUI.mix(color, 0xFFFFFFFF, h ? .3f : .18f), bottom = PhoneUI.mix(color, 0xFF000000, .3f);
        PhoneUI.gradient(g, k.x(), k.y() + (h ? 1 : 0), k.w(), k.h() - 1, Math.min(k.w(), k.h()) / 2, top, bottom);
        float s = Math.min(1.2f, k.h() / 12f);
        PhoneUI.centered(g, label, k.x() + k.w() / 2f, k.y() + (k.h() - 8 * s) / 2f + (h ? 1 : 0), labelColor, s);
    }

    /** Wood: a warm gradient with fine grain lines. */
    protected static void wood(GuiGraphics g, int x, int y, int w, int h, int r) {
        PhoneUI.gradient(g, x, y, w, h, r, 0xFF7A4A28, 0xFF4E2C16);
        java.util.Random rnd = new java.util.Random(7);
        for (int i = 0; i < h / 2; i++) {
            int yy = y + r / 2 + rnd.nextInt(Math.max(1, h - r));
            int len = 20 + rnd.nextInt(Math.max(1, w - 40));
            int xx = x + r / 2 + rnd.nextInt(Math.max(1, w - len - r));
            g.fill(xx, yy, xx + len, yy + 1, rnd.nextBoolean() ? 0x18000000 : 0x14FFE0B0);
        }
    }

    /** Brushed metal: light at the top, fine streaks across. */
    protected static void metal(GuiGraphics g, int x, int y, int w, int h, int r, int top, int bottom) {
        PhoneUI.gradient(g, x, y, w, h, r, top, bottom);
        java.util.Random rnd = new java.util.Random(11);
        for (int yy = y + 2; yy < y + h - 2; yy += 2) {
            g.fill(x + 3, yy, x + w - 3, yy + 1, rnd.nextBoolean() ? 0x0CFFFFFF : 0x0A000000);
        }
    }
}
